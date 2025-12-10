package ar.utn.ba.dds.front_tp.dto.output;

import java.util.Map;
import ar.utn.ba.dds.front_tp.dto.TipoCriterio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriterioDePertenenciaOutputDTO {
  private Long id; // Si viene null es porq es un criterio nuevo

  @NotBlank(message = "El nombre del criterio es obligatorio.")
  private String nombreCriterio;

  @NotNull(message = "El tipo de criterio es obligatorio")
  private TipoCriterio tipoCriterio;

  @NotEmpty(message = "Los parámetros son obligatorios.")
  private Map<String, Object> parametros;
}
