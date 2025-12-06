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
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
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
public class ColeccionesApiService {

  private static final Logger log = LoggerFactory.getLogger(ColeccionesApiService.class);
  private String coleccionesServiceUrl = "http://localhost:8081/metamapa";
  private final WebApiCallerService webApiCallerService;
  private final ColeccionMapper coleccionMapper;
  private final WebClient webClient;

  public ColeccionesApiService(WebApiCallerService webApiCallerService, ColeccionMapper coleccionMapper){
    this.webClient = WebClient.builder().baseUrl("http://localhost:8081/metamapa/colecciones").build();
    this.webApiCallerService = webApiCallerService;
    this.coleccionMapper = coleccionMapper;
  }

  // TODO: HAY 2 OPCIONES: generar un BaseApiClient donde se tengan estos 2 metodos y que todos lo hereden O copiar y pegar esto en todos los lugares que se utilice
  private Mono<Throwable> manejarError(ClientResponse response) {
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
              "Error sin detalle del servidor",
              List.of("Status code: " + status)
          );
          return mapToExceptionWithLogs(status, fallbackError);
        }));
  }

  private Mono<Throwable> mapToExceptionWithLogs(int status, ApiError err) {
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
        // Manejo 4xx
        .onStatus(HttpStatusCode::isError, response -> {
          log.warn("Error recibido. Status: {}", response.statusCode().value());
          return this.manejarError(response); // Llama al método centralizado (que mapea 4xx)
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
}