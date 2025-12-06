package ar.utn.ba.dds.front_tp.mappers;

import ar.utn.ba.dds.front_tp.dto.input.FuenteInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.FuenteOutputDTO;
import org.springframework.stereotype.Component;

@Component
public class FuenteMapper {

  public FuenteOutputDTO toFuenteOutputDTO (FuenteInputDTO fuenteInputDTO){
    if(fuenteInputDTO == null) return null;

    return FuenteOutputDTO.builder()
        .id(fuenteInputDTO.getId())
        .nombre(fuenteInputDTO.getNombre())
        .build();
  }
}
