package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.admin.DashboardSummaryDTO;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardApiService {

  private static final Logger log = LoggerFactory.getLogger(DashboardApiService.class);
  private final WebApiCallerService webApiCallerService;

  // Asumiremos que el servicio agregador expondrá esta URL. Añádela a tu properties.
  @Value("${dashboard.service.url}")
  private String dashboardServiceUrl;

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
}
