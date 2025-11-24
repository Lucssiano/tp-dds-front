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

  public void eliminarColeccion(Long id, String token) {
    try {
      // El backend espera: DELETE /metamapa/colecciones?id=123
      // Asumo que 'coleccionesServiceUrl' apunta a '.../metamapa/colecciones'
      // Si no, ajusta la URL.
      String url = coleccionesServiceUrl + "/colecciones?id=" + id;

      // Usamos el webApiCallerService (asegúrate de tener el método delete implementado ahí)
      webApiCallerService.delete(url, token);

    } catch (Exception e) {
      throw new RuntimeException("Error al eliminar la colección: " + e.getMessage());
    }
  }


  public ColeccionDTO obtenerColeccionPorId(Long id) {
    try {
      String url = coleccionesServiceUrl + "/colecciones?ids=" + id;
      List<ColeccionDTO> lista = webApiCallerService.getPublicList(url, ColeccionDTO.class);

      if (lista != null && !lista.isEmpty()) {
        return lista.get(0);
      }
      throw new RuntimeException("Colección no encontrada");
    } catch (Exception e) {
      throw new RuntimeException("Error al obtener la colección: " + e.getMessage());
    }
  }


  public void modificarColeccion(Long id, ColeccionInputDTO coleccionInput, String token) {
    try {
      String url = coleccionesServiceUrl + "/colecciones?id=" + id;
      // Usamos el nuevo método que acepta el token explícito
      webApiCallerService.putWithAuth(url, coleccionInput, Void.class, token);
    } catch (Exception e) {
      throw new RuntimeException("Error al modificar la colección: " + e.getMessage());
    }
  }

  public List<ColeccionDTO> obtenerUltimasColecciones(int cantidad) {
    try {
      // 1. Traemos todas (endpoint público)
      List<ColeccionDTO> todas = obtenerColecciones();

      if (todas == null || todas.isEmpty()) {
        return List.of();
      }

      // 2. Ordenamos por ID descendente (lo más nuevo primero) y limitamos
      return todas.stream()
          .sorted((c1, c2) -> c2.getId().compareTo(c1.getId())) // Descendente
          .limit(cantidad)
          .toList();

    } catch (Exception e) {
      // Si falla, devolvemos lista vacía para no romper el Home
      return List.of();
    }
  }
}