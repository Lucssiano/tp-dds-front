package ar.utn.ba.dds.front_tp.exceptions.api;

import ar.utn.ba.dds.front_tp.dto.input.ApiError;

// Clase concreta para el fallback
public class GeneralApiException extends ApiException {
  public GeneralApiException(int status, ApiError apiError) {
    super(status, apiError);
  }
}
