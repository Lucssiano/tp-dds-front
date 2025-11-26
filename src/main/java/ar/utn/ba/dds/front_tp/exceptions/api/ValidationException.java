package ar.utn.ba.dds.front_tp.exceptions.api;

import ar.utn.ba.dds.front_tp.dto.input.ApiError;

// 400 & 422: Para errores de campo.
public class ValidationException extends ApiException {
  public ValidationException(int status, ApiError apiError) { super(status, apiError); }
}