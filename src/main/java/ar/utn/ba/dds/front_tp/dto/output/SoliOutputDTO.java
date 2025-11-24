package ar.utn.ba.dds.front_tp.dto.output;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SoliOutputDTO {
  private Long id;
  private Long idHecho;
  private String hechoTitulo;
  private String justificacion;
  private EstadoSolicitud estado;
  private String usuario;
}
