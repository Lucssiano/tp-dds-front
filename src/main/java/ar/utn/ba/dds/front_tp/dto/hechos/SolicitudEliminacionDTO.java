package ar.utn.ba.dds.front_tp.dto.hechos;

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

  // Datos del hecho que se quiere eliminar
  private Long idHecho;
  private String tituloHecho;

  // Motivo de la solicitud
  private String motivo;

  // Usuario que hizo la solicitud
  private String usuario;
}
