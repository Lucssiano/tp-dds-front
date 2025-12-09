package ar.utn.ba.dds.front_tp.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SolicitudEliminacionInputDTO {
  private Long idHecho;

  @NotBlank(message = "La justificación es obligatoria.")
  @Size(min = 500, message = "La justificación es muy corta. Por favor, escribe al menos 500 caracteres para que podamos analizar el caso.")
  private String justificacion;

  private String usuario;
}
