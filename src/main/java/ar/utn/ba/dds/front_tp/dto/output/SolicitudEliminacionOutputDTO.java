package ar.utn.ba.dds.front_tp.dto.output;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SolicitudEliminacionOutputDTO {
  private Long idHecho;

  @NotBlank(message = "La justificación es obligatoria.")
  @Size(min = 500, message = "La justificación es muy corta. Por favor, escribe al menos 500 caracteres para que podamos analizar el caso.")
  @Size(max = 2000, message = "La justificación no puede superar los 2000 caracteres.")
  private String justificacion;

  private String usuario;
}
