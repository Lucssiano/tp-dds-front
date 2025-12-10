package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.editar.EditarHechoDTO;
import ar.utn.ba.dds.front_tp.dto.input.CategoriaInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.PageInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.CategoriaOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import ar.utn.ba.dds.front_tp.exceptions.api.GlobalBusinessException;
import ar.utn.ba.dds.front_tp.mappers.HechoMapper;
import ar.utn.ba.dds.front_tp.services.internal.HandlerExceptions;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class HechosApiService {
  private final WebClient webClient;
  private final WebApiCallerService webApiCallerService;
  private final String hechosServiceUrl = "http://localhost:8081/metamapa";
  private final String fuenteDinamicaUrl = "http://localhost:8083/fuente-dinamica";
  private final HechoMapper hechoMapper;
  private final HandlerExceptions handlerExceptions;

  @Autowired
  public HechosApiService(WebApiCallerService webApiCallerService, HechoMapper hechoMapper, HandlerExceptions handlerExceptions) {
    this.webClient = WebClient.builder().build();
    this.webApiCallerService = webApiCallerService;
    this.hechoMapper = hechoMapper;
    this.handlerExceptions = handlerExceptions;
  }
  @Autowired
  private HttpSession session;

  /**
   * Obtiene hechos, opcionalmente filtrados por modo y/o rango de fechas.
   * @param fechaDesde La fecha de inicio del rango.
   * @param fechaHasta La fecha de fin del rango.
   * @return Una lista de HechoInputDTO.
   */
  public List<HechoInputDTO> obtenerHechos(LocalDate fechaDesde, LocalDate fechaHasta, List<Long> categorias, List<Long> fuentes) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(hechosServiceUrl + "/hechos/paginado")
        .queryParam("page", 0)
        .queryParam("size", 100);

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

    String uriFinal = builder.toUriString();

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
          "El sistema externo no responde. No se pudieron obtener los hechos.",
          List.of(e.getMessage())
      );
    }
  }

  public HechoInputDTO obtenerHecho(Long id){
    return webClient.get()
        .uri(hechosServiceUrl + "/hechos/" + id)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> {
          log.warn("Error recibido. Status: {}", response.statusCode().value());
          return this.handlerExceptions.manejarError(response);
        })
        .bodyToMono(HechoInputDTO.class)
        .block();
  }

  public HechoOutputDTO crearHecho(EditarHechoDTO editarHechoDTO, String token) {
    HechoOutputDTO hechoOutputDTO = this.hechoMapper.toHechoOutputDTO(editarHechoDTO);

    try {
      return webClient.post()
          .uri(fuenteDinamicaUrl + "/hechos")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
          .bodyValue(hechoOutputDTO)
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> {
            log.warn("Error HTTP recibido. Status: {}", response.statusCode().value());
            return this.handlerExceptions.manejarError(response);
          })
          .bodyToMono(HechoOutputDTO.class)
          .block(); // <--- Aquí explota si está caído

    } catch (WebClientRequestException e) {
      log.error("🔥 Error de conexión con módulo externo: {}", e.getMessage());

      throw new GlobalBusinessException(
          503,
          "SERVICE_UNAVAILABLE", // Código para identificarlo
          "El sistema externo no responde. No se pudo guardar el hecho.",
          List.of(e.getMessage())
      );
    }
  }

  public Void editarHecho(Long id, EditarHechoDTO editarHechoDTO) {
    HechoOutputDTO hechoOutputDTO = this.hechoMapper.toHechoOutputDTO(editarHechoDTO);

    try {
      return webClient.put()
          .uri(hechosServiceUrl + "/hechos/" + id)
          .bodyValue(hechoOutputDTO)
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
          "SERVICE_UNAVAILABLE", // Código para identificarlo
          "El sistema externo no responde. No se pudo guardar el hecho.",
          List.of(e.getMessage())
      );
    }
  }

  public List<CategoriaOutputDTO> obtenerCategoriasOutput(){
    // Lógica igual a la que tenías, pero mapeando a CategoriaOutputDTO
    // Si usas el mismo endpoint, Spring lo mapeará bien si los nombres coinciden
    return webClient.get().uri(hechosServiceUrl +"/hechos/categorias")
        .retrieve().bodyToFlux(CategoriaOutputDTO.class).collectList().block();
  }

  public List<CategoriaInputDTO> obtenerCategorias(){
    try {
      return webClient.get()
          .uri(hechosServiceUrl +"/hechos/categorias")
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> {
            log.warn("Error recibido. Status: {}", response.statusCode().value());
            return this.handlerExceptions.manejarError(response);
          })
          .bodyToFlux(CategoriaInputDTO.class)
          .collectList()
          .block();
    } catch (WebClientRequestException e) {
      log.error("🔥 Error de conexión con módulo externo: {}", e.getMessage());

      throw new GlobalBusinessException(
          503,
          "SERVICE_UNAVAILABLE", // Código para identificarlo
          "El sistema externo no responde. No se pudieron obtener las categorías.",
          List.of(e.getMessage())
      );
    }
  }

  public HechoInputDTO obtenerUltimoHecho() {

    try {
      String url = hechosServiceUrl + "/hechos/ultimo";

      HechoInputDTO hecho = webClient.get()
          .uri(url)
          .retrieve()
          .bodyToMono(HechoInputDTO.class)
          .block();


      log.info("primer hecho: " + hecho);


      // Buscamos el ID más alto (el último creado)
      return hecho;

    } catch (Exception e) {
      // Logueamos pero no rompemos la app, devolvemos null y el Home no mostrará nada
      log.error("No se pudo obtener el hecho del día: {}", e.getMessage());
      return null;
    }
  }

  public List<HechoInputDTO> obtenerHechosUsuario(String usuario) {
    try {
      String url = UriComponentsBuilder.fromUriString(hechosServiceUrl + "/hechos/mis-hechos")
          .queryParam("usuario", usuario)
          .queryParam("page", 0)
          .queryParam("size", 20)
          .toUriString();
      var tipoRespuesta = new ParameterizedTypeReference<PageInputDTO<HechoInputDTO>>() {};

      PageInputDTO<HechoInputDTO> pagedResponse = webClient
          .get()
          .uri(url)
          .retrieve()
          .bodyToMono(tipoRespuesta)
          .block();

      return pagedResponse != null ? pagedResponse.content() : Collections.emptyList();
    } catch (Exception e) {
      log.error("No se pudieron obtener los hechos del usuario: " + usuario + " con error: " + e.getMessage());
      return Collections.emptyList();
    }
  }
}
