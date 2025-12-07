package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.input.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.ApiError;
import ar.utn.ba.dds.front_tp.dto.output.ColeccionOutputDTO;
import ar.utn.ba.dds.front_tp.exceptions.api.AutenticationException;
import ar.utn.ba.dds.front_tp.exceptions.api.AuthorizationException;
import ar.utn.ba.dds.front_tp.exceptions.api.GeneralApiException;
import ar.utn.ba.dds.front_tp.exceptions.api.InternalServerErrorException;
import ar.utn.ba.dds.front_tp.exceptions.api.ResourceNotFoundException;
import ar.utn.ba.dds.front_tp.exceptions.api.ValidationException;
import ar.utn.ba.dds.front_tp.mappers.ColeccionMapper;
import ar.utn.ba.dds.front_tp.services.internal.HandlerExceptions;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatusCode;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ColeccionesApiService {
  private String coleccionesServiceUrl = "http://localhost:8081/metamapa";
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

//  public ColeccionDTO crearColeccion(ColeccionInputDTO coleccionInput, String token) {
//    String url = coleccionesServiceUrl + "/colecciones";
//    log.info("Creando nueva colección en: {}", url);
//
//    ColeccionDTO coleccionDTO = coleccionMapper.toColeccionDTO(coleccionInput);
//    log.info("Criterios: " + coleccionDTO.getCriteriosDePertenencias());
//
//    try {
//      return webApiCallerService.postWithAuth(url, coleccionDTO, ColeccionDTO.class, token);
//    } catch (WebClientResponseException e) {
//      log.error("Error al crear la colección: {}", e.getResponseBodyAsString());
//      // Lanza una excepción runtime con el mensaje del backend
//      throw new RuntimeException("No se pudo crear la colección: " + e.getResponseBodyAsString(), e);
//
//    } catch (Exception e) {
//      log.error("Error inesperado al crear la colección: {}", e.getMessage());
//      throw new RuntimeException("No se pudo crear la colección. Causa: " + e.getMessage(), e);
//    }
//  }

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


  public void modificarColeccion(Long id, ColeccionInputDTO coleccionInput, String token) {
    try {
      String url = coleccionesServiceUrl + "/colecciones?id=" + id;
      // Usamos el nuevo método que acepta el token explícito
      webApiCallerService.putWithAuth(url, coleccionInput, Void.class, token);
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