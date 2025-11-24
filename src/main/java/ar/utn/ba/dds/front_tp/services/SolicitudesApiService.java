package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.hechos.input.SolicitudEliminacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.SoliOutputDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SolicitudesApiService {
  private static final Logger log = LoggerFactory.getLogger(SolicitudesApiService.class);
  private String baseUrl = "http://localhost:8081/metamapa/solicitudes";
  private final WebClient webClient;

  public SolicitudesApiService() {
    this.webClient = WebClient.builder().baseUrl(baseUrl).build();
  }

  public SoliOutputDTO crearSolicitudEliminacion(SolicitudEliminacionInputDTO soli){
    log.info("usuario en service solicitud: "+ soli.getUsuario());
    return webClient.post()
        .bodyValue(soli)
        .retrieve()
        .bodyToMono(SoliOutputDTO.class)
        .block();
  }
}
