package ar.utn.ba.dds.front_tp.dto.hechos;

import ar.utn.ba.dds.front_tp.dto.output.EstadoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudEliminacionDTO implements Serializable {
  private Long id;
  private Long idHecho;
  private String hechoTitulo;
  private String justificacion;
  private EstadoSolicitud estado;
  private String usuario;
}
