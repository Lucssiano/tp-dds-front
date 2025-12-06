package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.input.FuenteInputDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FuentesApiService {

  private final WebClient webClient;

  public FuentesApiService() {
    this.webClient = WebClient.builder().baseUrl("http://localhost:8081/metamapa/fuentes").build();
  }

  public List<FuenteInputDTO> obtenerFuentes() {
    return webClient.get().retrieve().bodyToFlux(FuenteInputDTO.class).collectList().block();
  }
}
