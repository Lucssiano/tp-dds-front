package ar.utn.ba.dds.front_tp.dto.input;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Soporta error general, errores por campo y errores globales
@JsonInclude(JsonInclude.Include.NON_EMPTY) // Oculta campos vacíos o nulos en el JSON
public record ApiError(
    String code,
    String message,
    Map<String, String> fields,
    List<String> details,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp
) {
  // Compact Constructor: Garantiza que nunca haya nulos internamente (en los records el constructor me permite poner validaciones)
  public ApiError {
    // Si pasan null, lo convertimos a vacío inmutable
    fields = (fields != null) ? fields : Map.of();
    details = (details != null) ? details : List.of();
    // Si no pasan fecha, ponemos la actual
    timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
  }

  // Factory Methods
  // Constructor para errores simples (solo mensaje y código)
  public static ApiError of(String code, String message) {
    return new ApiError(code, message, Map.of(), List.of(), LocalDateTime.now());
  }

  // Constructor para errores sin campos específicos (globales)
  public static ApiError of(String code, String message, List<String> details) {
    return new ApiError(code, message, Map.of(), details, LocalDateTime.now());
  }

  // Constructor para errores que se pueden asociar a campos
  public static ApiError of(String code, String message, Map<String, String> fields) {
    return new ApiError(code, message, fields, List.of(), LocalDateTime.now());
  }
}