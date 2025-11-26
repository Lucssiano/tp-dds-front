package ar.utn.ba.dds.front_tp.exceptions.api;

import ar.utn.ba.dds.front_tp.dto.input.ApiError;

public class InternalServerErrorException extends ApiException {
  public InternalServerErrorException(int status, ApiError apiError) {
    super(status, apiError);
  }
}