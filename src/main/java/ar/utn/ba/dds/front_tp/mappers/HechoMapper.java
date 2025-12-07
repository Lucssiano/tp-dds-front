package ar.utn.ba.dds.front_tp.mappers;

import ar.utn.ba.dds.front_tp.dto.editar.EditarHechoDTO;
import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import org.springframework.stereotype.Component;

@Component
public class HechoMapper {
  public EditarHechoDTO toEditarHechoDTO(HechoInputDTO hechoInputDTO){
    return EditarHechoDTO.builder()
        .titulo(hechoInputDTO.getTitulo())
        .descripcion(hechoInputDTO.getDescripcion())
        .categoria(hechoInputDTO.getCategoria())
        .multimedia(hechoInputDTO.getMultimedia())
        .latitud(hechoInputDTO.getUbicacionDTO().getLatitud())
        .longitud(hechoInputDTO.getUbicacionDTO().getLongitud())
        .fecha(hechoInputDTO.getFechaHecho())
        .estado(hechoInputDTO.getEstado())
        .usuario(hechoInputDTO.getUsuario())
        .build();
  }

  public HechoOutputDTO toHechoOutputDTO(EditarHechoDTO editarHechoDTO){
    return HechoOutputDTO.builder()
        .titulo(editarHechoDTO.getTitulo())
        .descripcion(editarHechoDTO.getDescripcion())
        .categoria(editarHechoDTO.getCategoria())
        .multimedia(editarHechoDTO.getMultimedia())
        .latitud(editarHechoDTO.getLatitud())
        .longitud(editarHechoDTO.getLongitud())
        .fecha(editarHechoDTO.getFecha())
        .usuario(editarHechoDTO.getUsuario())
        .build();
  }
}
