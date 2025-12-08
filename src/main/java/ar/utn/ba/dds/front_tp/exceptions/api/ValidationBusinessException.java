package ar.utn.ba.dds.front_tp.exceptions.api;

import ar.utn.ba.dds.front_tp.dto.input.ApiError;

// 400 & 422: Para errores de campo.
public class ValidationBusinessException extends ApiException {
  public ValidationBusinessException(int status, ApiError apiError) { super(status, apiError); }
}