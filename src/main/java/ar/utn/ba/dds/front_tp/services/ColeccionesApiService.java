package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionDTO;
import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ColeccionesApiService {

  private static final Logger log = LoggerFactory.getLogger(ColeccionesApiService.class);
  private final WebApiCallerService webApiCallerService;

  @Value("${colecciones.service.url}")
  private String coleccionesServiceUrl;

  public List<ColeccionDTO> obtenerColecciones() {
    try {
      String url = coleccionesServiceUrl + "/colecciones";
      log.info("Obteniendo colecciones desde (público): {}", url);

      List<ColeccionDTO> colecciones = webApiCallerService.getPublicList(url, ColeccionDTO.class);

      if (colecciones != null) {
        for (int i = 0; i < colecciones.size(); i++) {
          colecciones.get(i).setImagenUrl("https://picsum.photos/300/200?random=" + (i + 1));
        }
      }

      return colecciones;

    } catch (Exception e) {
      log.error("Error al obtener las colecciones: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  public ColeccionDTO crearColeccion(ColeccionInputDTO coleccionInput, String token) {
    try {
      String url = coleccionesServiceUrl + "/colecciones";
      log.info("Creando nueva colección en: {}", url);

      return webApiCallerService.postWithAuth(url, coleccionInput, ColeccionDTO.class, token);

    } catch (Exception e) {
      log.error("Error al crear la colección: {}", e.getMessage());
      // Lanzamos la excepción para que el controlador la maneje y muestre un error
      throw new RuntimeException("No se pudo crear la colección. Causa: " + e.getMessage(), e);
    }
  }
}