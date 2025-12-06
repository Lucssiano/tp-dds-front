package ar.utn.ba.dds.front_tp.mappers;

import ar.utn.ba.dds.front_tp.dto.hechos.EditarHechoDTO;
import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import org.springframework.stereotype.Component;

@Component
public class HechoMapper {
  public EditarHechoDTO toEditarHechoDTO(HechoInputDTO hechoInputDTO){
    return EditarHechoDTO.builder()
        .titulo(hechoInputDTO.getTitulo())
        .descripcion(hechoInputDTO.getDescripcion())
        .categoria(hechoInputDTO.getCategoria())
        .multimedia(hechoInputDTO.getMultimedia())
        .latitud(hechoInputDTO.getUbicacionOutputDTO().getLatitud())
        .longitud(hechoInputDTO.getUbicacionOutputDTO().getLongitud())
        .fecha(hechoInputDTO.getFechaHecho())
        .build();
  }
}
