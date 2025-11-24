package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.services.ColeccionesApiService;
import ar.utn.ba.dds.front_tp.services.HechosApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

  private final ColeccionesApiService coleccionesApiService;
  private final HechosApiService hechosApiService;

  @GetMapping({"/", "/home"})
  public String mostrarHome(Model model) {

    // 1. Obtenemos las 2 últimas colecciones reales del Backend
    List<ColeccionDTO> recientes = coleccionesApiService.obtenerUltimasColecciones(2);

    // 2. Mapeamos a la estructura que espera el HTML (agregando imagen fake)
    var coleccionesParaVista = recientes.stream().map(dto -> Map.of(
        "id", dto.getId(),
        "titulo", dto.getTitulo(),
        "descripcion", dto.getDescripcion() != null ? dto.getDescripcion() : "Sin descripción",
        // Generamos una imagen aleatoria basada en el ID para que siempre sea la misma para esa colección
        "imagenUrl", "https://picsum.photos/300/200?random=" + dto.getId()
    )).toList();

    // 3. Pasamos al modelo
    model.addAttribute("coleccionesDestacadas", coleccionesParaVista);

    HechoDTO ultimoHecho = hechosApiService.obtenerUltimoHecho();

    if (ultimoHecho != null) {
      String imagenUrl = "https://picsum.photos/600/400?random=" + ultimoHecho.getId();

      var hechoDestacadoMap = Map.of(
          "id", ultimoHecho.getId(),
          "titulo", ultimoHecho.getTitulo(),
          // Truncamos descripción si es muy larga
          "descripcion", (ultimoHecho.getDescripcion() != null && ultimoHecho.getDescripcion().length() > 150)
              ? ultimoHecho.getDescripcion().substring(0, 150) + "..."
              : (ultimoHecho.getDescripcion() != null ? ultimoHecho.getDescripcion() : ""),
          "imagenUrl", imagenUrl
      );

      model.addAttribute("hechoDestacado", hechoDestacadoMap);
    } else {
      model.addAttribute("hechoDestacado", null);
    }
    return "home";
  }
}
