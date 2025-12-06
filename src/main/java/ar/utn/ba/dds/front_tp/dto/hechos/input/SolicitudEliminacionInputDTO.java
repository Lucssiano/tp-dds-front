package ar.utn.ba.dds.front_tp.dto.hechos.input;

import lombok.Data;

@Data
public class SolicitudEliminacionInputDTO {
  private Long idHecho;
  private String justificacion;
  private String usuario;
}
