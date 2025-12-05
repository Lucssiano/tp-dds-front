package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.hechos.CategoriaDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.CrearHechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.EditarHechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.dto.input.PageInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.mappers.HechoMapper;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import jakarta.servlet.http.HttpSession;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class HechosApiService {
  private static final Logger log = LoggerFactory.getLogger(HechosApiService.class);
  private final WebClient webClient;
  private final WebApiCallerService webApiCallerService;
  private final String hechosServiceUrl = "http://localhost:8081/metamapa";
  private final String fuenteDinamicaUrl = "http://localhost:8083/fuente-dinamica/hechos";
  private final HechoMapper hechoMapper;

  @Autowired
  public HechosApiService(WebApiCallerService webApiCallerService,
            HechoMapper hechoMapper) {
    this.webClient = WebClient.builder().build();
    this.webApiCallerService = webApiCallerService;
    this.hechoMapper = hechoMapper;
  }
  @Autowired
  private HttpSession session;

  /**
   * Obtiene hechos, opcionalmente filtrados por modo y/o rango de fechas.
   * @param modo Puede ser "CURADO", "IRRESTRICTO" o null.
   * @param fechaDesde La fecha de inicio del rango.
   * @param fechaHasta La fecha de fin del rango.
   * @return Una lista de HechoDTO.
   */
  public List<HechoDTO> obtenerHechos(String modo, LocalDate fechaDesde, LocalDate fechaHasta) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(hechosServiceUrl + "/hechos/paginado")
        .queryParam("page", 0)
        .queryParam("size", 100);

    if (modo != null && !modo.isEmpty()) {
      builder.queryParam("modo", modo);
    }

    // --- AQUÍ ESTÁ EL CAMBIO ---
    if (fechaDesde != null) {
      // Usamos el nombre que el backend final espera
      builder.queryParam("fechaAcontecimientoDesde", fechaDesde.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
    if (fechaHasta != null) {
      // Usamos el nombre que el backend final espera
      builder.queryParam("fechaAcontecimientoHasta", fechaHasta.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    String urlFinal = builder.toUriString();
    log.info("Llamando a la URL de hechos: {}", urlFinal);

    try {
      // Para evitar el warning deberia hacer algo similar a lo q hago abajo con webclient pero hay q modificar la implementacion del webApiCallerService
        log.info("Antes del llamado api");
      PageInputDTO<HechoDTO> pagedResponse = webApiCallerService.get(urlFinal, PageInputDTO.class);

      return pagedResponse.getContent();

    } catch (RuntimeException e) {
      // Lógica de error y llamada pública
      if (e.getMessage() != null && e.getMessage().contains("No hay token de acceso disponible")) {
        log.warn("No hay token: usando llamada pública sin autenticación");

        // Definición explícita del tipo genérico para WebClient (sino me lanza un warning porq no conoce el tipo de dato que va dentro de PageInputDTO)
        ParameterizedTypeReference<PageInputDTO<HechoDTO>> typeRef = new ParameterizedTypeReference<PageInputDTO<HechoDTO>>() {};
        PageInputDTO<HechoDTO> pagedResponse = webClient.get()
            .uri(urlFinal)
            .retrieve()
            .bodyToMono(typeRef) // bodyToMono, no bodyToFlux
            .block();

        return pagedResponse != null ? pagedResponse.getContent() : Collections.emptyList();
      }
      throw e;
    }
  }

  public List<HechoDTO> obtenerHechosColeccion(Long id){
    try {
      return webClient.get()
          .uri(hechosServiceUrl + "/colecciones/" + id +"/hechos")
          .retrieve()
          .bodyToFlux(HechoDTO.class)
          .collectList()
          .block();
    } catch (Exception e) {
      log.warn("No se pudieron obtener los hechos de la coleccion: "+id+" por error"+e.getMessage());
      return null;
    }
  }

  public HechoDTO obtenerHecho(Long id){
    return webClient.get()
        .uri(hechosServiceUrl + "/hechos/" + id)
        .retrieve()
        .bodyToMono(HechoDTO.class)
        .block();
  }

  public HechoOutputDTO crearHecho(CrearHechoDTO payload, String token) {

    log.info("Enviando hecho. Título: {}", payload.getHecho().getTitulo());
    log.info("Lat: {}, Long: {}", payload.getHecho().getLatitud(), payload.getHecho().getLongitud());
    log.info("Token incluido en body: {}", payload.getAccessToken() != null ? "SI" : "NO");
    //log.info("Primer imagen: {} ", payload.getHecho().getMultimedia().get(0));
    return webApiCallerService.postWithAuth(
        fuenteDinamicaUrl,        // URL 8083
        payload.getHecho(),       // Body: HechoOutputDTO
        HechoOutputDTO.class,     // Respuesta esperada
        token                     // Token para el Header
    );
  }

  public Void editarHecho(Long id, HechoDTO hechoDTO) {

    EditarHechoDTO editarHechoDTO = hechoMapper.toEditarHechoDTO(hechoDTO);

    return webClient.post()
        .uri(hechosServiceUrl + "/hechos/" + id + "/editar")
        .bodyValue(editarHechoDTO)
        .retrieve()
        .bodyToMono(Void.class)
        .block();
  }

  public List<CategoriaDTO> obtenerCategorias(){
      try{
        String url = hechosServiceUrl +"/hechos/categorias";
        List<CategoriaDTO> categorias = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(CategoriaDTO.class)
                .collectList()
                .block();

        return categorias;

      }catch (Exception e){
          log.error("No se pudieron obtener las categorias {}", e.getMessage());
          return null;
      }
  }
  public HechoDTO obtenerUltimoHecho() {

    try {
      String url = hechosServiceUrl + "/hechos/ultimo";

      HechoDTO hecho = webClient.get()
          .uri(url)
          .retrieve()
          .bodyToMono(HechoDTO.class)
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

  public List<HechoDTO> obtenerHechosUsuario(String usuario){
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(hechosServiceUrl + "/hechos")
        .queryParam("usuario", usuario);
    try {
      return webClient.get()
          .uri(builder.toUriString())
          .retrieve()
          .bodyToFlux(HechoDTO.class)
          .collectList()
          .block();
    }catch (Exception e){
      log.error("No se pudieron obtener los hechos del usuario: "+ usuario + " con error: " + e.getMessage());
      return null;
    }
  }
}
