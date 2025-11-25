package ar.utn.ba.dds.front_tp.mappers;

import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionDTO;
import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionInputDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ColeccionMapper {
  public ColeccionDTO toColeccionDTO(ColeccionInputDTO input) {

    if (input == null) {
      return null;
    }

    ColeccionDTO dto = new ColeccionDTO();

    dto.setTitulo(input.getTitulo());
    dto.setDescripcion(input.getDescripcion());

    // Copias defensivas para evitar problemas si luego modificás las listas
    dto.setCriteriosDePertenencias(
        input.getCriteriosDePertenencias() != null
            ? new ArrayList<>(input.getCriteriosDePertenencias())
            : new ArrayList<>()
    );

    dto.setFuentes(
        input.getFuentes() != null
            ? new ArrayList<>(input.getFuentes())
            : new ArrayList<>()
    );

    dto.setAlgoritmoConsenso(input.getAlgoritmoConsenso());

    // id queda en null porque aún no existe
    // imagenUrl ya tiene un default en el DTO

    return dto;
  }

}
