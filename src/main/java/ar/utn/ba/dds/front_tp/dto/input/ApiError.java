package ar.utn.ba.dds.front_tp.dto.input;

import java.util.List;
import java.util.Map;

// Soporta error general, errores por campo y errores globales
public record ApiError(
    String code,
    String message,
    Map<String, String> fields,
    List<String> details
) {
  // Para errores sin campos específicos (globales)
  public static ApiError of(String code, String message, List<String> details) {
    return new ApiError(code, message, Map.of(), details);
  }

  // Para errores que se pueden asociar a campos
  public static ApiError withFields(String code, String message, Map<String, String> fields) {
    return new ApiError(code, message, fields, List.of());
  }
}
