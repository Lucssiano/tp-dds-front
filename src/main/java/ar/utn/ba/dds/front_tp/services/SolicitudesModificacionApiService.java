package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.input.SolicitudModificacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.SolicitudModificacionOutputDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Service
public class SolicitudesModificacionApiService {
  private final WebClient webClient;
  private static final Logger log = LoggerFactory.getLogger(SolicitudesApiService.class);

  public SolicitudesModificacionApiService(){
    this.webClient = WebClient.builder().baseUrl("http://localhost:8081/metamapa/solicitudes-modif").build();
  }

  public SolicitudModificacionOutputDTO crearSolicitudModificacion(Long id, HechoInputDTO hechoInputDTO){
    HechoOutputDTO hechoOutputDTO = HechoOutputDTO.builder()
        .titulo(hechoInputDTO.getTitulo())
        .descripcion(hechoInputDTO.getDescripcion())
        .categoria(hechoInputDTO.getCategoria())
        .multimedia(hechoInputDTO.getMultimedia())
        .fecha(hechoInputDTO.getFechaHecho())
        .latitud(hechoInputDTO.getUbicacionOutputDTO().getLatitud())
        .longitud(hechoInputDTO.getUbicacionOutputDTO().getLongitud())
        .usuario(hechoInputDTO.getUsuario())
        .build();

    SolicitudModificacionOutputDTO solicitudModificacionOutputDTO = SolicitudModificacionOutputDTO.builder()
        .hechoId(id)
        .hecho(hechoOutputDTO)
        .build();

    return webClient.post()
        .bodyValue(solicitudModificacionOutputDTO)
        .retrieve()
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
