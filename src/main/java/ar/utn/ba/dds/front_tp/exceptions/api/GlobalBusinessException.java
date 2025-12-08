package ar.utn.ba.dds.front_tp.exceptions.api;

import ar.utn.ba.dds.front_tp.dto.input.ApiError;

import java.util.List;

public class GlobalBusinessException extends ApiException {
  // CONSTRUCTOR 1: Cuando el error viene del Backend (ya tenés el objeto)
  public GlobalBusinessException(int status, ApiError apiError) {
    super(status, apiError);
  }

  // CONSTRUCTOR 2: Cuando el error es Local (Conexión, lógica interna)
  // Fabrica el ApiError internamente para mantener la consistencia.
  public GlobalBusinessException(int status, String code, String message, List<String> details) {
    super(status, ApiError.of(code, message, details));
  }
}
