package ar.utn.ba.dds.front_tp.exceptions.api;

import ar.utn.ba.dds.front_tp.dto.input.ApiError;
import lombok.Data;
import lombok.Getter;

@Getter
public abstract class ApiException extends RuntimeException {
  protected final int status;
  protected final ApiError apiError;

  public ApiException(int status, ApiError apiError) {
    super(apiError != null ? apiError.message() : "Error de la API con estado: " + status);
    this.status = status;
    this.apiError = apiError;
  }
}