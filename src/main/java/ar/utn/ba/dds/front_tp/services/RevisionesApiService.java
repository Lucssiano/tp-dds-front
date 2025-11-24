package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.SolicitudEliminacionDTO;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Service
public class RevisionesApiService {

  private static final Logger log = LoggerFactory.getLogger(RevisionesApiService.class);
  private final WebApiCallerService webApiCallerService;
  private String baseUrl = "http://localhost:8081/metamapa";
  private final WebClient webClient;
  public RevisionesApiService(WebApiCallerService webApiCallerService){
    this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    this.webApiCallerService = webApiCallerService;
  }

  public List<HechoDTO> obtenerHechosPendientes(String token) {
    try {
      String url = baseUrl + "/hechos/pendientes";

      log.info("Obteniendo hechos pendientes desde: {}", url);
      return webApiCallerService.getListWithAuth(url, token, HechoDTO.class);
    } catch (Exception e) {
      log.error("Error al obtener hechos pendientes: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  public List<SolicitudEliminacionDTO> obtenerSolicitudesPendientes(String token) {
    try {
      String url = baseUrl + "/solicitudes/pendientes"; // Ajusta la URL
      log.info("Obteniendo solicitudes pendientes desde: {}", url);
      return webApiCallerService.getListWithAuth(url, token, SolicitudEliminacionDTO.class);
    } catch (Exception e) {
      log.error("Error al obtener solicitudes: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  public void aprobarHecho(Long id, String token) {
    enviarAccionHecho(id, "aprobar", token);
  }

  public void rechazarHecho(Long id, String token) {
    enviarAccionHecho(id, "rechazar", token);
  }

  // Agrego estos para las solicitudes de eliminación según tu HTML
  public void aceptarSolicitud(Long id, String token) {
    enviarAccionSolicitud(id, "aceptar", token);
  }

  public void rechazarSolicitud(Long id, String token) {
    enviarAccionSolicitud(id, "rechazar", token);
  }

  private void enviarAccionHecho(Long id, String accion, String token) {
    try {
      log.info("ID: " + id + " - Accion: " + accion);
      webClient.post()
          .uri("/hechos/" + id + "/" + accion)
          .retrieve()
          .bodyToMono(Void.class)
          .block();
    } catch (Exception e) {
      throw new RuntimeException("Error al " + accion + " el hecho: " + e.getMessage());
    }
  }

  private void enviarAccionSolicitud(Long id, String accion, String token) {
    try {
      String url = baseUrl + "/solicitudes/" + id + "/" + accion;
      webApiCallerService.postWithAuth(url, null, Void.class, token);
    } catch (Exception e) {
      throw new RuntimeException("Error al " + accion + " la solicitud: " + e.getMessage());
    }
  }
}