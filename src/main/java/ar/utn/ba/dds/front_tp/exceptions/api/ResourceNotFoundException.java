package ar.utn.ba.dds.front_tp.exceptions.api;

import ar.utn.ba.dds.front_tp.dto.input.ApiError;

// 404: Recurso No Encontrado (Mostrar vista 404)
public class ResourceNotFoundException extends ApiException {
  public ResourceNotFoundException(int status, ApiError apiError) { super(status, apiError); }
}
