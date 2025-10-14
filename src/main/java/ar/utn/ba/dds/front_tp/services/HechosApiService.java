package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.hechos.CrearHechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import jakarta.servlet.http.HttpSession;
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

import java.util.List;
import java.util.Map;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class HechosApiService {
  private static final Logger log = LoggerFactory.getLogger(HechosApiService.class);
  private final WebClient webClient;
  private final WebApiCallerService webApiCallerService;
  private final String hechosServiceUrl;

  @Autowired
  public HechosApiService(WebApiCallerService webApiCallerService,
            @Value("${hechos.service.url}") String hechosServiceUrl) {
    this.webClient = WebClient.builder().build();
    this.webApiCallerService = webApiCallerService;
    this.hechosServiceUrl = hechosServiceUrl;
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

  public HechoOutputDTO crearHecho(HechoOutputDTO hecho, String token) {

    log.info("Lat: {}, Long: {}", hecho.getLatitud(), hecho.getLongitud());

    CrearHechoDTO crearHechoDTO = CrearHechoDTO.builder()
        .hecho(hecho)
        .accessToken(token)
        .build();
    log.info("Fecha que se envía: {}", crearHechoDTO.getHecho().getFecha());
    HechoOutputDTO response = webClient
        .post()
        .uri(hechosServiceUrl + "/hechos")
        .bodyValue(crearHechoDTO)
        .retrieve()
        .bodyToMono(HechoOutputDTO.class)
        .block();

    if (response == null) {
      throw new RuntimeException("Error al crear el hecho en el servicio externo.");
    }

    return response;
  }

}
