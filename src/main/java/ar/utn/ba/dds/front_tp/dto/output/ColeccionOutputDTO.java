package ar.utn.ba.dds.front_tp.dto.output;

import ar.utn.ba.dds.front_tp.dto.TipoAlgoritmoConsenso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ColeccionOutputDTO {
  private Long id;

  @NotBlank(message = "El título es obligatorio.")
  @Size(max = 100, message = "El título no puede superar los 100 caracteres.")
  private String titulo;

  @NotBlank(message = "La descripción es obligatoria.")
  @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
  private String descripcion;

  @NotEmpty(message = "Debe definir al menos un criterio de pertenencia.")
  @Valid
  private List<CriterioDePertenenciaOutputDTO> criteriosDePertenencias;

  @NotEmpty(message = "Debe seleccionar al menos una fuente para la colección.")
  private List<Long> fuentesIds;

  private TipoAlgoritmoConsenso algoritmoConsenso;
}
