package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.hechos.CrearHechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

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

  public List<HechoDTO> obtenerHechos() {
    String url = hechosServiceUrl + "/hechos";
    try {
      List<HechoDTO> hechoDTOS = webApiCallerService.getList(url, HechoDTO.class);
      log.info("titulo primer hecho: " + hechoDTOS.stream().findFirst().get().getTitulo());
      return hechoDTOS;
    }catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().contains("No hay token de acceso disponible")) {
        log.warn("No hay token: usando llamada pública sin autenticación");
        // llamada pública sin header Authorization
        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToFlux(HechoDTO.class)
            .collectList()
            .block();
      }
      throw e;
    } catch (Exception e){
      throw new RuntimeException("Error general al obtener hechos: " + e.getMessage());
    }
  }
  public HechoDTO crearHecho(HechoDTO hecho, String token){
    CrearHechoDTO crearHechoDTO = CrearHechoDTO.builder().hecho(hecho).accessToken(token).build();
    HechoDTO response = webApiCallerService.post(hechosServiceUrl, crearHechoDTO, HechoDTO.class);

    if (response == null) {
      throw new RuntimeException("Error al crear el hecho en el servicio externo.");
    }

    return response;
  }
}
