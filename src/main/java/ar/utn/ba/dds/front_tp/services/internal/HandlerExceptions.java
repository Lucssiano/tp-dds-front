package ar.utn.ba.dds.front_tp.services.internal;

import ar.utn.ba.dds.front_tp.dto.input.ApiError;
import ar.utn.ba.dds.front_tp.exceptions.api.AutenticationException;
import ar.utn.ba.dds.front_tp.exceptions.api.AuthorizationException;
import ar.utn.ba.dds.front_tp.exceptions.api.GeneralApiException;
import ar.utn.ba.dds.front_tp.exceptions.api.InternalServerErrorException;
import ar.utn.ba.dds.front_tp.exceptions.api.ResourceNotFoundException;
import ar.utn.ba.dds.front_tp.exceptions.api.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class HandlerExceptions {
  public Mono<Throwable> manejarError(ClientResponse response) {
    int status = response.statusCode().value();
    log.info("🌐 Iniciando manejo de error HTTP. Status recibido: {}", status);

    return response.bodyToMono(ApiError.class)
        // CASO A: El backend devolvió un JSON con el error
        .flatMap(apiError -> {
          log.info("API Error Body deserializado (Código/Mensaje): {} / {}",
              apiError.code(), apiError.message());
          return mapToExceptionWithLogs(status, apiError);
        })
        // CASO B: El backend falló sin body (o body vacío)
        .switchIfEmpty(Mono.defer(() -> {
          log.warn("API Error Body vacío. Generando error genérico.");
          ApiError fallbackError = ApiError.of(
              String.valueOf(status),
              "Error sin detalle del servidor"
          );
          return mapToExceptionWithLogs(status, fallbackError);
        }));
  }

  public Mono<Throwable> mapToExceptionWithLogs(int status, ApiError err) {
    String apiCode = err.code() != null ? err.code() : "N/A";

    if (status == 400 || status == 422) {
      log.error("Lanzando ValidationException (Status {}). Código API: {}", status, apiCode);
      return Mono.error(new ValidationException(status, err));
    }
    else if (status == 401) {
      log.error("Lanzando AutenticationException (Status 401).");
      return Mono.error(new AutenticationException(status, err));
    }
    else if (status == 403) {
      log.error("Lanzando AuthorizationException (Status 403).");
      return Mono.error(new AuthorizationException(status, err));
    }
    else if (status == 404) {
      log.error("Lanzando ResourceNotFoundException (Status 404).");
      return Mono.error(new ResourceNotFoundException(status, err));
    }
    else if (status >= 500) {
      log.error("Lanzando InternalServerErrorException (Status {}).", status);
      return Mono.error(new InternalServerErrorException(status, err));
    }
    else {
      log.error("Lanzando GeneralApiException (Status {}).", status);
      return Mono.error(new GeneralApiException(status, err));
    }
  }
}
