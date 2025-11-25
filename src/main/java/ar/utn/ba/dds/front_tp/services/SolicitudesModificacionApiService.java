package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.SolicitudModificacionOutputDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SolicitudesModificacionApiService {
  private final WebClient webClient;

  public SolicitudesModificacionApiService(){
    this.webClient = WebClient.builder().baseUrl("http://localhost:8081/metamapa/solicitudes-modif").build();
  }

  public SolicitudModificacionOutputDTO crearSolicitudModificacion(Long id, HechoDTO hechoDTO){
    HechoOutputDTO hechoOutputDTO = HechoOutputDTO.builder()
        .titulo(hechoDTO.getTitulo())
        .descripcion(hechoDTO.getDescripcion())
        .categoria(hechoDTO.getCategoria())
        .multimedia(hechoDTO.getMultimedia())
        .fecha(hechoDTO.getFechaHecho())
        .latitud(hechoDTO.getUbicacionOutputDTO().getLatitud())
        .longitud(hechoDTO.getUbicacionOutputDTO().getLongitud())
        .usuario(hechoDTO.getUsuario())
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
}
