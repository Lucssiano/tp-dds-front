package ar.utn.ba.dds.front_tp.mappers;

import ar.utn.ba.dds.front_tp.dto.hechos.EditarHechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import org.springframework.stereotype.Component;

@Component
public class HechoMapper {
  public EditarHechoDTO toEditarHechoDTO(HechoDTO hechoDTO){
    return EditarHechoDTO.builder()
        .titulo(hechoDTO.getTitulo())
        .descripcion(hechoDTO.getDescripcion())
        .categoria(hechoDTO.getCategoria())
        .multimedia(hechoDTO.getMultimedia())
        .latitud(hechoDTO.getUbicacionOutputDTO().getLatitud())
        .longitud(hechoDTO.getUbicacionOutputDTO().getLongitud())
        .fecha(hechoDTO.getFechaHecho())
        .build();
  }
}
