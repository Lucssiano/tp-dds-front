package ar.utn.ba.dds.front_tp.mappers;

import ar.utn.ba.dds.front_tp.dto.input.CriterioDePertenenciaInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.FuenteInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.ColeccionOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.CriterioDePertenenciaOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.FuenteOutputDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ColeccionMapper {

  public ColeccionOutputDTO toColeccionOutputDTO(ColeccionInputDTO input) {

    if (input == null) {
      return null;
    }

    ColeccionOutputDTO dto = new ColeccionOutputDTO();

    dto.setTitulo(input.getTitulo());
    dto.setDescripcion(input.getDescripcion());

    // Copias defensivas para evitar problemas si luego modificás las listas
    dto.setCriteriosDePertenencias(
        input.getCriteriosDePertenencias() != null
            ? input.getCriteriosDePertenencias().stream().map(this::toCriterioDePertenenciaOutputDTO).toList()
            : new ArrayList<>()
    );

    dto.setFuentesIds(
        input.getFuentes() != null
            ? input.getFuentes().stream().map(FuenteInputDTO::getId).toList()
            : new ArrayList<>()
    );

    dto.setAlgoritmoConsenso(input.getAlgoritmoConsenso());

    // id queda en null porque aún no existe
    // imagenUrl ya tiene un default en el ColeccionOutputDTO

    return dto;
  }

  public CriterioDePertenenciaOutputDTO toCriterioDePertenenciaOutputDTO (CriterioDePertenenciaInputDTO criterioDePertenenciaInputDTO){
    if(criterioDePertenenciaInputDTO == null ) return null;
    return CriterioDePertenenciaOutputDTO.builder()
        .id(criterioDePertenenciaInputDTO.getId())
        .nombreCriterio(criterioDePertenenciaInputDTO.getNombreCriterio())
        .tipoCriterio(criterioDePertenenciaInputDTO.getTipoCriterio())
        .parametros(criterioDePertenenciaInputDTO.getParametros())
        .build();
  }
}
