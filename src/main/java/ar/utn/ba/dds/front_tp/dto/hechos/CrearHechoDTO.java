package ar.utn.ba.dds.front_tp.dto.hechos;

import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class CrearHechoDTO {
  private HechoOutputDTO hecho;
  private String accessToken;
}
