package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.editar.EditarHechoDTO;
import ar.utn.ba.dds.front_tp.dto.input.SolicitudModificacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.SolicitudModificacionOutputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.exceptions.api.GlobalBusinessException;
import ar.utn.ba.dds.front_tp.mappers.HechoMapper;
import ar.utn.ba.dds.front_tp.services.internal.HandlerExceptions;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.List;

@Service
@Slf4j
public class SolicitudesModificacionApiService {
  private final WebClient webClient;

  private final WebApiCallerService webApiCallerService;

  @Autowired
  private HechoMapper hechoMapper;

  @Autowired
  private HandlerExceptions handlerExceptions;

  public SolicitudesModificacionApiService(WebApiCallerService webApiCallerService){
    this.webApiCallerService = webApiCallerService;
    this.webClient = WebClient.builder().baseUrl("http://localhost:8081/metamapa/solicitudes-modif").build();
  }

  public void crearSolicitudModificacion(Long id, EditarHechoDTO editarHechoDTO){
    HechoOutputDTO hechoOutputDTO = this.hechoMapper.toHechoOutputDTO(editarHechoDTO);

    SolicitudModificacionOutputDTO solicitudModificacionOutputDTO = SolicitudModificacionOutputDTO.builder()
        .hechoId(id)
        .hecho(hechoOutputDTO)
        .build();

    String accessToken = null;

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
      try {
        AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
        accessToken = authData.getAccessToken();

        log.info("Token: "+ accessToken);
      } catch (Exception e) {
        // Logueamos pero no rompemos el flujo, seguimos intentando
        System.err.println("Advertencia: No se pudo extraer token");
      }
    }

    try {
      webClient.post()
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .bodyValue(solicitudModificacionOutputDTO)
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> {
            log.warn("Error recibido. Status: {}", response.statusCode().value());
            return this.handlerExceptions.manejarError(response);
          })
          .bodyToMono(SolicitudModificacionOutputDTO.class)
          .block();
    } catch (WebClientRequestException e) {
      log.error("🔥 Error de conexión con módulo externo: {}", e.getMessage());

      throw new GlobalBusinessException(
          503,
          "SERVICE_UNAVAILABLE",
          "El sistema externo no responde. No se pudo crear la solicitud de modificación.",
          List.of(e.getMessage())
      );
    }
  }

  public List<SolicitudModificacionInputDTO> obtenerSolicitudesModificacionPendientes() {
    try {
      return webClient.get()
          .uri("/pendientes")
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> {
            log.warn("Error recibido. Status: {}", response.statusCode().value());
            return this.handlerExceptions.manejarError(response);
          })
          .bodyToFlux(SolicitudModificacionInputDTO.class)
          .collectList()
          .block();
    } catch (WebClientRequestException e) {
      log.error("🔥 Error de conexión con módulo externo: {}", e.getMessage());

      throw new GlobalBusinessException(
          503,
          "SERVICE_UNAVAILABLE",
          "El sistema externo no responde. No se puieron obtener las solicitudes de modificación pendientes.",
          List.of(e.getMessage())
      );
    }
  }

  public SolicitudModificacionInputDTO obtenerSolicitud(Long id) {
    try {
      return webClient.get()
          .uri("/" + id)
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> {
            log.warn("Error recibido. Status: {}", response.statusCode().value());
            return this.handlerExceptions.manejarError(response);
          })
          .bodyToMono(SolicitudModificacionInputDTO.class)
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

  public void aceptarSolicitudModificacion(Long id) {
    enviarAccion(id, "aceptar");
  }

  public void rechazarSolicitudModificacion(Long id) {
    enviarAccion(id, "rechazar");
  }

  private void enviarAccion(Long id, String accion) {
    String accessToken = null;

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
      try {
        AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
        accessToken = authData.getAccessToken();

        log.info("Token: "+ accessToken);
      } catch (Exception e) {
        // Logueamos pero no rompemos el flujo, seguimos intentando
        System.err.println("Advertencia: No se pudo extraer token");
      }
    }
    try {
      log.info("ID: " + id + " - Accion: " + accion);
      webClient.post()
          .uri("/" + id + "/" + accion)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> {
            log.warn("Error recibido. Status: {}", response.statusCode().value());
            return this.handlerExceptions.manejarError(response);
          })
          .bodyToMono(Void.class)
          .block();
    } catch (WebClientRequestException e) {
      log.error("🔥 Error de conexión con módulo externo: {}", e.getMessage());

      throw new GlobalBusinessException(
          503,
          "SERVICE_UNAVAILABLE",
          "El sistema externo no responde. No se pudo ejecutar la acción.",
          List.of(e.getMessage())
      );
    }
  }
}
