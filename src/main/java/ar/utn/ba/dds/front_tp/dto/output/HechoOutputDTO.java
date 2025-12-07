package ar.utn.ba.dds.front_tp.dto.output;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class HechoOutputDTO {
  @NotNull(message = "El titulo es obligatorio.")
  private String titulo;
  @NotNull(message = "La descripcion es obligatoria.")
  private String descripcion;
  private String categoria;
  private List<String> multimedia;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
  @NotNull(message = "La fecha del hecho es obligatoria.")
  @PastOrPresent(message = "La fecha y hora del hecho no pueden ser futuras.")
  private LocalDateTime fecha;
  @NotNull(message = "La ubicación es obligatoria. Por favor, selecciona un punto en el mapa.")
  private BigDecimal latitud;
  @NotNull(message = "La ubicación es obligatoria. Por favor, selecciona un punto en el mapa.")
  private BigDecimal longitud;
  @JsonIgnore
  private List<String> etiquetas;
  private String usuario;
}




