package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.hechos.FuentesInputDTO;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FuentesApiService {

  private final WebClient webClient;

  @Autowired
  public FuentesApiService() {
    this.webClient = WebClient.builder().baseUrl("http://localhost:8081/metamapa").build();
  }

  public FuentesInputDTO obtenerFuentes() {
    return webClient.get().uri("/fuentes").retrieve().bodyToMono(FuentesInputDTO.class).block();
  }
}
