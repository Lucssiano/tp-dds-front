package ar.utn.ba.dds.front_tp.dto.output;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SolicitudModificacionOutputDTO {
  private Long hechoId;
  private HechoOutputDTO hecho;
}
