package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionDTO;
import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.ApiError;
import ar.utn.ba.dds.front_tp.exceptions.api.ApiException;
import ar.utn.ba.dds.front_tp.exceptions.api.AutenticationException;
import ar.utn.ba.dds.front_tp.exceptions.api.AuthorizationException;
import ar.utn.ba.dds.front_tp.exceptions.api.GeneralApiException;
import ar.utn.ba.dds.front_tp.exceptions.api.InternalServerErrorException;
import ar.utn.ba.dds.front_tp.exceptions.api.ResourceNotFoundException;
import ar.utn.ba.dds.front_tp.exceptions.api.ValidationException;
import ar.utn.ba.dds.front_tp.mappers.ColeccionMapper;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatusCode;

import java.util.Collections;
import java.util.List;

@Service
public class ColeccionesApiService {

  private static final Logger log = LoggerFactory.getLogger(ColeccionesApiService.class);
  @Value("${colecciones.service.url}")
  private String coleccionesServiceUrl;
  private final WebApiCallerService webApiCallerService;
  private final ColeccionMapper coleccionMapper;
  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  public ColeccionesApiService(WebApiCallerService webApiCallerService, ColeccionMapper coleccionMapper, ObjectMapper objectMapper){
    this.webClient = WebClient.builder().baseUrl("http://localhost:8081/metamapa/colecciones").build();
    this.webApiCallerService = webApiCallerService;
    this.coleccionMapper = coleccionMapper;
    this.objectMapper = objectMapper;
  }

  // Asumimos que ObjectMapper está disponible en esta clase (inyectado o instanciado)
// private final ObjectMapper objectMapper = new ObjectMapper();

//  private Mono<Throwable> manejarError(ClientResponse response) {
//    return response.bodyToMono(String.class)
//        .doOnNext(body -> log.error("ERROR RAW BODY => {}", body)) // 🔥 ESTO SÍ SE VE
//        .flatMap(body -> {
//
//          try {
//            ApiError apiError = objectMapper.readValue(body, ApiError.class);
//
//            //log.error("ApiError mapeado: code={}, message={}, fields={}", apiError.getCode(), apiError.getMessage(), apiError.getFields());
//
//            return Mono.error(new ValidationException(response.statusCode().value(), apiError));
//          } catch (Exception e) {
//            log.error("Error parseando error JSON. Body original: {}", body);
//            return Mono.error(
//                new RuntimeException(
//                    "Error desconocido del backend: " + body
//                )
//            );
//          }
//        });
//  }


  private Mono<Throwable> manejarError(ClientResponse response) {
    int status = response.statusCode().value();
    log.info("🌐 Iniciando manejo de error HTTP. Status recibido: {}", status);

    // 🚨 PASO 1: Leer el cuerpo como String (Esto garantiza que el stream no se rompa por codec)
    return response.bodyToMono(ApiError.class)
        .flatMap(err -> {
          log.info("Entre al flat map");
          String apiCode = err != null ? err.code() : "N/A";
          // LOG 2: Registramos el contenido del ApiError (o si estaba vacío)
          log.info("API Error Body deserializado (Código/Mensaje): {} / {}",
              apiCode,
              err != null ? err.message() : "Cuerpo vacío");

          // Mapeo basado en el status
          if (status == 400 || status == 422) {
            // Errores de validación con fields (Mapeo de 400/422)
            log.error("Lanzando ValidationException (Status {}). Código API: {}", status, err != null ? err.code() : "N/A");
            return Mono.error(new ValidationException(status, err));
          } else if (status == 401) {
            // Autenticación
            log.error("Lanzando AutenticationException (Status 401).");
            return Mono.error(new AutenticationException(status, err));
          } else if (status == 403) {
            // Autorización
            log.error("Lanzando AuthorizationException (Status 403).");
            return Mono.error(new AuthorizationException(status, err));
          } else if (status == 404) {
            // Recurso no encontrado
            log.error("Lanzando ResourceNotFoundException (Status 404).");
            return Mono.error(new ResourceNotFoundException(status, err));
          } else if (status >= 500) {
            // Errores 5xx (Aunque deben manejarse en el onStatus 5xx, es un buen fallback)
            log.error("Lanzando InternalServerErrorException (Status {}).", status);
            return Mono.error(new InternalServerErrorException(status, err));
          } else {
            // Fallback para cualquier otro 4xx no mapeado
            log.error("Lanzando GeneralApiException (Status {}).", status);
            return Mono.error(new GeneralApiException(status, err));
          }
        });
  }

  public List<ColeccionDTO> obtenerColecciones() {
    try {
      String url = coleccionesServiceUrl + "/colecciones";
      log.info("Obteniendo colecciones desde (público): {}", url);

      List<ColeccionDTO> colecciones = webApiCallerService.getPublicList(url, ColeccionDTO.class);

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

  public Mono<ColeccionDTO> crearColeccion(ColeccionInputDTO coleccionInput, String token) {
    log.info("Iniciando creación de colección: {}", coleccionInput.getTitulo());

    // Convertimos el DTO de entrada al formato esperado por el backend (si es necesario)
    ColeccionDTO coleccionDTO = coleccionMapper.toColeccionDTO(coleccionInput);

    return webClient.post()
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .bodyValue(coleccionDTO)
        .retrieve()
        // Manejo 4xx
        .onStatus(HttpStatusCode::is4xxClientError, response -> {
          log.warn("Error 4xx recibido (CLIENTE). Status: {}", response.statusCode().value());
          return this.manejarError(response); // Llama al método centralizado (que mapea 4xx)
        })
        // Manejo 5xx
        .onStatus(HttpStatusCode::is5xxServerError, response -> {
          int status = response.statusCode().value();
          log.error("Error 5xx recibido (SERVIDOR). Status: {}", status);

          // Intenta deserializar el ApiError (aunque no es lo usual en 5xx)
          return response.bodyToMono(ApiError.class)
              .defaultIfEmpty(null)
              .flatMap(err -> {
                // Lanzamos la excepción específica para el servidor.
                return Mono.error(new InternalServerErrorException(status, err));
              });
        })
        .bodyToMono(ColeccionDTO.class);
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


  public ColeccionDTO obtenerColeccionPorId(Long id) {
    try {
      String url = coleccionesServiceUrl + "/colecciones?ids=" + id;
      List<ColeccionDTO> lista = webApiCallerService.getPublicList(url, ColeccionDTO.class);

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

  public List<ColeccionDTO> obtenerUltimasColecciones(int cantidad) {
    try {
      // 1. Traemos todas (endpoint público)
      List<ColeccionDTO> todas = obtenerColecciones();

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