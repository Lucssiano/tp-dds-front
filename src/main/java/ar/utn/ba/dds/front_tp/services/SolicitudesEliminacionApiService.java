package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.hechos.SolicitudEliminacionDTO;
import ar.utn.ba.dds.front_tp.dto.input.SolicitudEliminacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.SolicitudEliminacionOutputDTO;
import ar.utn.ba.dds.front_tp.exceptions.api.GlobalBusinessException;
import ar.utn.ba.dds.front_tp.services.internal.HandlerExceptions;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.Collections;
import java.util.List;

@Service
public class SolicitudesEliminacionApiService {
  private static final Logger log = LoggerFactory.getLogger(SolicitudesEliminacionApiService.class);
  private String baseUrl = "http://localhost:8081/metamapa/solicitudes";
  private final WebClient webClient;
  private final WebApiCallerService webApiCallerService;
  private final HandlerExceptions handlerExceptions;

  public SolicitudesEliminacionApiService(WebApiCallerService webApiCallerService, HandlerExceptions handlerExceptions) {
    this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    this.webApiCallerService = webApiCallerService;
    this.handlerExceptions = handlerExceptions;
  }

  public SolicitudEliminacionInputDTO crearSolicitud(SolicitudEliminacionOutputDTO solicitudEliminacionOutputDTO) {
    try {
      return webClient.post()
          .uri(baseUrl)
          .bodyValue(solicitudEliminacionOutputDTO)
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> {
            log.warn("Error recibido. Status: {}", response.statusCode().value());
            return this.handlerExceptions.manejarError(response);
          })
          .bodyToMono(SolicitudEliminacionInputDTO.class)
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

  public List<SolicitudEliminacionInputDTO> obtenerSolicitudesPendientes(String token) {
    try {
      String url = baseUrl + "/pendientes";
      log.info("Obteniendo solicitudes pendientes desde: {}", url);
      return webApiCallerService.getListWithAuth(url, token, SolicitudEliminacionInputDTO.class);
    } catch (Exception e) {
      log.error("Error al obtener solicitudes: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  public SolicitudEliminacionInputDTO obtenerSolicitud(Long id) {
    try {
      return webClient.get()
          .uri("/" + id)
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> {
            log.warn("Error recibido. Status: {}", response.statusCode().value());
            return this.handlerExceptions.manejarError(response);
          })
          .bodyToMono(SolicitudEliminacionInputDTO.class)
          .block();
    } catch (WebClientRequestException e) {
      log.error("🔥 Error de conexión con módulo externo: {}", e.getMessage());

      throw new GlobalBusinessException(
          503,
          "SERVICE_UNAVAILABLE",
          "El sistema externo no responde. No se pudo obtener la solicitud de modificación.",
          List.of(e.getMessage())
      );
    }
  }

  public void aceptarSolicitud(Long id, String token) {
    enviarAccionSolicitud(id, "aceptar", token);
  }

  public void rechazarSolicitud(Long id, String token) {
    enviarAccionSolicitud(id, "rechazar", token);
  }

  private void enviarAccionSolicitud(Long id, String accion, String token) {
    try {
      String url = baseUrl + "/" + id + "/" + accion;
      webApiCallerService.postWithAuth(url, null, Void.class, token);
    } catch (Exception e) {
      throw new RuntimeException("Error al " + accion + " la solicitud: " + e.getMessage());
    }
  }
}
