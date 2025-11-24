package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.hechos.CrearHechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.EditarHechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class HechosApiService {
  private static final Logger log = LoggerFactory.getLogger(HechosApiService.class);
  private final WebClient webClient;
  private final WebApiCallerService webApiCallerService;
  private final String hechosServiceUrl = "http://localhost:8081/metamapa";
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
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(hechosServiceUrl + "/hechos")
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
      return webApiCallerService.getList(urlFinal, HechoDTO.class);
    } catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().contains("No hay token de acceso disponible")) {
        log.warn("No hay token: usando llamada pública sin autenticación");
        return webClient.get()
            .uri(urlFinal)
            .retrieve()
            .bodyToFlux(HechoDTO.class)
            .collectList()
            .block();
      }
      throw e;
    }
  }

  public HechoDTO obtenerHecho(Long id){
    return webClient.get()
        .uri(hechosServiceUrl + "/hechos/" + id)
        .retrieve()
        .bodyToMono(HechoDTO.class)
        .block();
  }

  // CAMBIO 1: El parámetro ahora es CrearHechoDTO (el wrapper que manda el controller)

  public HechoOutputDTO crearHecho(CrearHechoDTO payload, String token) {

    log.info("Enviando hecho. Título: {}", payload.getHecho().getTitulo());
    log.info("Lat: {}, Long: {}", payload.getHecho().getLatitud(), payload.getHecho().getLongitud());
    log.info("Token incluido en body: {}", payload.getAccessToken() != null ? "SI" : "NO");

    String url = hechosServiceUrl + "/hechos";

    return webApiCallerService.postWithAuth(url, payload, HechoOutputDTO.class, token);
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
}
