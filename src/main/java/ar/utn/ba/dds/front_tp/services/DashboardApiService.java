package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.admin.DashboardSummaryDTO;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class DashboardApiService {

  private static final Logger log = LoggerFactory.getLogger(DashboardApiService.class);
  private final WebApiCallerService webApiCallerService;

  // Asumiremos que el servicio agregador expondrá esta URL. Añádela a tu properties.
  @Value("${dashboard.service.url}")
  private String dashboardServiceUrl;

  // 1. Declaramos el WebClient propio de este servicio
  private final WebClient webClient;

  // 2. Constructor manual para inicializar todo
  public DashboardApiService(WebApiCallerService webApiCallerService,
                             @Value("${dashboard.service.url}") String dashboardServiceUrl) {
    this.webApiCallerService = webApiCallerService;
    this.dashboardServiceUrl = dashboardServiceUrl;

    // 3. Configuración del buffer a 16MB para soportar subida de archivos CSV
    final int size = 16 * 1024 * 1024;
    final ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
        .build();

    this.webClient = WebClient.builder()
        .exchangeStrategies(strategies)
        .build();
  }

  public DashboardSummaryDTO getSummary(String token) {
    try {
      String url = dashboardServiceUrl + "/admin/summary";
      log.info("Obteniendo resumen del dashboard desde: {}", url);

      return webApiCallerService.getWithAuth(url, token, DashboardSummaryDTO.class);

    } catch (Exception e) {
      log.error("Error al obtener el resumen del dashboard: {}", e.getMessage());
      // Si falla, devuelvo un DTO con ceros para que la página no se rompa.
      return new DashboardSummaryDTO();
    }
  }

  public void importarHechos(MultipartFile file, String token) {
    try {
      String url = dashboardServiceUrl + "/admin/importar-hechos"; // Ajusta según tu backend

      MultipartBodyBuilder builder = new MultipartBodyBuilder();
      builder.part("file", file.getResource());

      webClient.post() // Usamos webClient directo para tener control del Multipart
          .uri(url)
          .header("Authorization", "Bearer " + token)
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(builder.build()))
          .retrieve()
          .toBodilessEntity()
          .block();

    } catch (Exception e) {
      throw new RuntimeException("Fallo la importación: " + e.getMessage());
    }
  }
}
