package ar.utn.ba.dds.front_tp.dto.hechos;

import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CrearHechoDTO {
  private HechoOutputDTO hecho;
  private String accessToken;
}
