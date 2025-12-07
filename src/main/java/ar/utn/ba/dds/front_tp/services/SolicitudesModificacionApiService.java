package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.editar.EditarHechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.input.SolicitudModificacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.SolicitudModificacionOutputDTO;
import ar.utn.ba.dds.front_tp.mappers.HechoMapper;
import ar.utn.ba.dds.front_tp.services.internal.HandlerExceptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class SolicitudesModificacionApiService {
  private final WebClient webClient;

  @Autowired
  private HechoMapper hechoMapper;

  @Autowired
  private HandlerExceptions handlerExceptions;

  public SolicitudesModificacionApiService(){
    this.webClient = WebClient.builder().baseUrl("http://localhost:8081/metamapa/solicitudes-modif").build();
  }

  public void crearSolicitudModificacion(Long id, EditarHechoDTO editarHechoDTO){
    HechoOutputDTO hechoOutputDTO = this.hechoMapper.toHechoOutputDTO(editarHechoDTO);

    SolicitudModificacionOutputDTO solicitudModificacionOutputDTO = SolicitudModificacionOutputDTO.builder()
        .hechoId(id)
        .hecho(hechoOutputDTO)
        .build();

    webClient.post()
        .bodyValue(solicitudModificacionOutputDTO)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> {
          log.warn("Error recibido. Status: {}", response.statusCode().value());
          return this.handlerExceptions.manejarError(response);
        })
        .bodyToMono(SolicitudModificacionOutputDTO.class)
        .block();
  }

  public List<SolicitudModificacionInputDTO> obtenerSolicitudesModificacionPendientes() {
    try {
      return webClient.get()
          .uri("/pendientes")
          .retrieve()
          .bodyToFlux(SolicitudModificacionInputDTO.class)
          .collectList()
          .block();
    } catch (Exception e) {
      log.error("Error al obtener solicitudes: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  public void aceptarSolicitudModificacion(Long id) {
    enviarAccion(id, "aceptar");
  }

  public void rechazarSolicitudModificacion(Long id) {
    enviarAccion(id, "rechazar");
  }

  private void enviarAccion(Long id, String accion) {
    try {
      log.info("ID: " + id + " - Accion: " + accion);
      webClient.post()
          .uri("/" + id + "/" + accion)
          .retrieve()
          .bodyToMono(Void.class)
          .block();
    } catch (Exception e) {
      throw new RuntimeException("Error al " + accion + " el hecho: " + e.getMessage());
    }
  }
}
