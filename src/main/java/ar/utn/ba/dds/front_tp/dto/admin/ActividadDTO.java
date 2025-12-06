package ar.utn.ba.dds.front_tp.dto.admin;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActividadDTO {

  // Campos que vienen directamente del Backend
  private String tipo;        // Ej: "NUEVO_HECHO", "SOLICITUD_BAJA"
  private String descripcion; // El título del hecho o de la colección
  private String autor;       // El email o el rol del autor

  // La fecha debe coincidir en formato con el Backend para la deserialización
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  private LocalDateTime fecha;

  // --- Getters para Thymeleaf (el HTML usa estos nombres) ---
  public String getAccion() { return tipo; }

  public String getTituloHecho() { return descripcion; }

  public String getFuente() { return autor; }
}
