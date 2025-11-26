package ar.utn.ba.dds.front_tp.exceptions.api;

import ar.utn.ba.dds.front_tp.dto.input.ApiError;

// 401: Autenticación (Redirección a login)
public class AutenticationException extends ApiException {
  public AutenticationException(int status, ApiError apiError) { super(status, apiError); }
}
