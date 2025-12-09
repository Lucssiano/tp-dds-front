package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.input.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.PageInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.ColeccionOutputDTO;
import ar.utn.ba.dds.front_tp.exceptions.api.GlobalBusinessException;
import ar.utn.ba.dds.front_tp.mappers.ColeccionMapper;
import ar.utn.ba.dds.front_tp.services.internal.HandlerExceptions;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatusCode;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ColeccionesApiService {
  private final String coleccionesServiceUrl = "http://localhost:8081/metamapa";
  private final WebApiCallerService webApiCallerService;
  private final ColeccionMapper coleccionMapper;
  private final WebClient webClient;
  private final HandlerExceptions handlerExceptions;

  public ColeccionesApiService(WebApiCallerService webApiCallerService, ColeccionMapper coleccionMapper, HandlerExceptions handlerExceptions){
    this.webClient = WebClient.builder().baseUrl("http://localhost:8081/metamapa/colecciones").build();
    this.webApiCallerService = webApiCallerService;
    this.coleccionMapper = coleccionMapper;
    this.handlerExceptions = handlerExceptions;
  }

  public List<ColeccionInputDTO> obtenerColecciones() {
    try {
      String url = coleccionesServiceUrl + "/colecciones";
      log.info("Obteniendo colecciones desde (público): {}", url);

      List<ColeccionInputDTO> colecciones = webApiCallerService.getPublicList(url, ColeccionInputDTO.class);

      if (colecciones != null) {
        for (int i = 0; i < colecciones.size(); i++) {
          colecciones.get(i).setImagenUrl("https://picsum.photos/300/200?random=" + (i + 1));
        }
      }

      return colecciones;

    } catch (Exception e) {
      log.error("Error al obtener las colecciones: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  public List<HechoInputDTO> obtenerHechosColeccion(Long id, String modo, LocalDate fechaDesde, LocalDate fechaHasta, List<Long> categorias, List<Long> fuentes) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(coleccionesServiceUrl + "/colecciones/{id}/hechos")
        .queryParam("page", 0)
        .queryParam("size", 100);

    if (modo != null) {
      builder.queryParam("modoNavegacion", modo);
    }
    if (fechaDesde != null) {
      builder.queryParam("fechaAcontecimientoDesde", fechaDesde);
    }
    if (fechaHasta != null) {
      builder.queryParam("fechaAcontecimientoHasta", fechaHasta);
    }
    if (categorias != null && !categorias.isEmpty()) {
      builder.queryParam("categorias", categorias);
    }
    if (fuentes != null && !fuentes.isEmpty()) {
      builder.queryParam("fuentes", fuentes);
    }

    String uriFinal = builder.buildAndExpand(id).toUriString();

    try {
      var tipoRespuesta = new ParameterizedTypeReference<PageInputDTO<HechoInputDTO>>() {};

      PageInputDTO<HechoInputDTO> pagedResponse = webClient
          .get()
          .uri(uriFinal)
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> {
            log.warn("Error recibido. Status: {}", response.statusCode().value());
            return this.handlerExceptions.manejarError(response);
          })
          .bodyToMono(tipoRespuesta)
          .block();

      return pagedResponse != null ? pagedResponse.content() : Collections.emptyList();

    } catch (
      WebClientRequestException e) {
      log.error("🔥 Error de conexión con módulo externo: {}", e.getMessage());

      throw new GlobalBusinessException(
          503,
          "SERVICE_UNAVAILABLE",
          "El sistema externo no responde. No se pudieron obtener los hechos de la colección.",
          List.of(e.getMessage())
      );
    }
  }

  public Mono<ColeccionInputDTO> crearColeccion(ColeccionOutputDTO coleccionOutputDTO, String token) {
    log.info("Iniciando creación de colección: {}", coleccionOutputDTO.getTitulo());

    return webClient.post()
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .bodyValue(coleccionOutputDTO)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> {
          log.warn("Error recibido. Status: {}", response.statusCode().value());
          return this.handlerExceptions.manejarError(response);
        })
        .bodyToMono(ColeccionInputDTO.class);
  }


  public void eliminarColeccion(Long id, String token) {
    try {
      // El backend espera: DELETE /metamapa/colecciones?id=123
      // Asumo que 'coleccionesServiceUrl' apunta a '.../metamapa/colecciones'
      // Si no, ajusta la URL.
      String url = coleccionesServiceUrl + "/colecciones?id=" + id;

      // Usamos el webApiCallerService (asegúrate de tener el método delete implementado ahí)
      webApiCallerService.delete(url, token);

    } catch (Exception e) {
      throw new RuntimeException("Error al eliminar la colección: " + e.getMessage());
    }
  }


  public ColeccionInputDTO obtenerColeccionPorId(Long id) {
    try {
      String url = coleccionesServiceUrl + "/colecciones?ids=" + id;
      List<ColeccionInputDTO> lista = webApiCallerService.getPublicList(url, ColeccionInputDTO.class);

      if (lista != null && !lista.isEmpty()) {
        return lista.get(0);
      }
      throw new RuntimeException("Colección no encontrada");
    } catch (Exception e) {
      throw new RuntimeException("Error al obtener la colección: " + e.getMessage());
    }
  }


  public void modificarColeccion(Long id, ColeccionOutputDTO coleccionOutput, String token) {
    try {
      String url = coleccionesServiceUrl + "/colecciones/" + id;
      // Usamos el nuevo método que acepta el token explícito
      webApiCallerService.putWithAuth(url, coleccionOutput, Void.class, token);
    } catch (Exception e) {
      throw new RuntimeException("Error al modificar la colección: " + e.getMessage());
    }
  }

  public List<ColeccionInputDTO> obtenerUltimasColecciones(int cantidad) {
    try {
      // 1. Traemos todas (endpoint público)
      List<ColeccionInputDTO> todas = obtenerColecciones();

      if (todas == null || todas.isEmpty()) {
        return List.of();
      }

      // 2. Ordenamos por ID descendente (lo más nuevo primero) y limitamos
      return todas.stream()
          .sorted((c1, c2) -> c2.getId().compareTo(c1.getId())) // Descendente
          .limit(cantidad)
          .toList();

    } catch (Exception e) {
      // Si falla, devolvemos lista vacía para no romper el Home
      return List.of();
    }
  }

  public List<ColeccionOutputDTO> obtenerColeccionesOutput() {
    // Asumiendo que webApiCallerService puede manejar el cambio de clase
    // O usa webClient directo como en FuentesApiService
    return webClient.get()
        .uri(coleccionesServiceUrl + "/colecciones") // Ajusta la URL si es necesario
        .retrieve()
        .bodyToFlux(ColeccionOutputDTO.class)
        .collectList()
        .block();
  }
}