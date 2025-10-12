package ar.utn.ba.dds.front_tp.dto.hechos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class CrearHechoDTO {
  private HechoDTO hecho;
  private String accessToken;
}
