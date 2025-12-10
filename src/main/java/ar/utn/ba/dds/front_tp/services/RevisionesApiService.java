package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.SolicitudEliminacionDTO;
import ar.utn.ba.dds.front_tp.dto.input.PageInputDTO;
import ar.utn.ba.dds.front_tp.services.internal.HandlerExceptions;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Service
public class RevisionesApiService {

  private static final Logger log = LoggerFactory.getLogger(RevisionesApiService.class);
  private final WebApiCallerService webApiCallerService;
  private String baseUrl = "http://localhost:8081/metamapa";
  private final WebClient webClient;
  private final HandlerExceptions handlerExceptions;

  public RevisionesApiService(WebApiCallerService webApiCallerService, HandlerExceptions handlerExceptions){
    this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    this.webApiCallerService = webApiCallerService;
    this.handlerExceptions = handlerExceptions;
  }

  public List<HechoInputDTO> obtenerHechosPendientes(String token) {
    try {
      String url = UriComponentsBuilder.fromUriString(baseUrl + "/hechos/pendientes")
          .queryParam("page", 0)
          .queryParam("size", 100)
          .toUriString();

      log.info("Obteniendo hechos pendientes desde: {}", url);
      var tipoRespuesta = new ParameterizedTypeReference<PageInputDTO<HechoInputDTO>>() {};

      PageInputDTO<HechoInputDTO> pagedResponse = webClient
          .get()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> {
            log.warn("Error recibido. Status: {}", response.statusCode().value());
            return this.handlerExceptions.manejarError(response);
          })
          .bodyToMono(tipoRespuesta)
          .block();

      return pagedResponse != null ? pagedResponse.content() : Collections.emptyList();
    } catch (Exception e) {
      log.error("Error al obtener hechos pendientes: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  public void aprobarHecho(Long id, String token) {
    enviarAccionHecho(id, "aprobar", token);
  }

  public void rechazarHecho(Long id, String token) {
    enviarAccionHecho(id, "rechazar", token);
  }

  private void enviarAccionHecho(Long id, String accion, String token) {
    try {
      log.info("ID: " + id + " - Accion: " + accion);
      webClient.post()
          .uri("/hechos/" + id + "/" + accion)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
          .retrieve()
          .bodyToMono(Void.class)
          .block();
    } catch (Exception e) {
      throw new RuntimeException("Error al " + accion + " el hecho: " + e.getMessage());
    }
  }


}