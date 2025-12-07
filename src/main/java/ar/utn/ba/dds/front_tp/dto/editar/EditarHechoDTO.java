package ar.utn.ba.dds.front_tp.dto.editar;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class EditarHechoDTO {
  @NotBlank(message = "El título es obligatorio.")
  @Size(max = 100, message = "El título no puede superar los 100 caracteres.")
  private String titulo;

  @NotBlank(message = "La descripción es obligatoria.")
  @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
  private String descripcion;

  @NotBlank(message = "La categoría es obligatoria.")
  @Size(max = 100, message = "La categoría no puede superar los 100 caracteres.")
  private String categoria;

  private List<String> multimedia;

  @NotNull(message = "La ubicación es obligatoria.")
  @DecimalMin(value = "-90.0", message = "La latitud no puede ser menor a -90.")
  @DecimalMax(value = "90.0", message = "La latitud no puede ser mayor a 90.")
  private BigDecimal latitud;

  @NotNull(message = "La ubicación es obligatoria.")
  @DecimalMin(value = "-180.0", message = "La longitud no puede ser menor a -180.")
  @DecimalMax(value = "180.0", message = "La longitud no puede ser mayor a 180.")
  private BigDecimal longitud;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
  @NotNull(message = "La fecha es obligatoria.")
  @PastOrPresent(message = "La fecha no puede ser posterior a la fecha y hora actual.")
  private LocalDateTime fecha;

  private String estado;
  private String usuario;
}
