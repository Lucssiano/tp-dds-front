package ar.utn.ba.dds.front_tp.exceptions.api;

import ar.utn.ba.dds.front_tp.dto.input.ApiError;

// 403: Autorización (Mostrar error de acceso)
public class AuthorizationException extends ApiException {
  public AuthorizationException(int status, ApiError apiError) { super(status, apiError); }
}