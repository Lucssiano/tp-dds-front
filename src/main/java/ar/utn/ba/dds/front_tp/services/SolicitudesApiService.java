package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.input.SolicitudEliminacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.SoliOutputDTO;
import ar.utn.ba.dds.front_tp.exceptions.api.GlobalBusinessException;
import ar.utn.ba.dds.front_tp.services.internal.HandlerExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.List;

@Service
public class SolicitudesApiService {
  private static final Logger log = LoggerFactory.getLogger(SolicitudesApiService.class);
  private String baseUrl = "http://localhost:8081/metamapa/solicitudes";
  private final WebClient webClient;
  private final HandlerExceptions handlerExceptions;

  public SolicitudesApiService(HandlerExceptions handlerExceptions) {
    this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    this.handlerExceptions = handlerExceptions;
  }

  public SoliOutputDTO crearSolicitudEliminacion(SolicitudEliminacionInputDTO soli) {
    try {
      return webClient.post()
          .uri(baseUrl)
          .bodyValue(soli)
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> {
            log.warn("Error recibido. Status: {}", response.statusCode().value());
            return this.handlerExceptions.manejarError(response);
          })
          .bodyToMono(SoliOutputDTO.class)
          .block();
    } catch (
        WebClientRequestException e) {
      log.error("🔥 Error de conexión con módulo externo: {}", e.getMessage());

      throw new GlobalBusinessException(
          503,
          "SERVICE_UNAVAILABLE", // Código para identificarlo
          "El sistema externo no responde. No se pudo crear la solicitud de eliminación.",
          List.of(e.getMessage())
      );
    }
  }
}
