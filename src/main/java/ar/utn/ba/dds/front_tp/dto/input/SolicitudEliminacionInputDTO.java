package ar.utn.ba.dds.front_tp.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SolicitudEliminacionInputDTO {
  private Long id;
  private Long idHecho;
  private String hechoTitulo;
  private String justificacion;
  private String estado;
  private String usuario;
}
