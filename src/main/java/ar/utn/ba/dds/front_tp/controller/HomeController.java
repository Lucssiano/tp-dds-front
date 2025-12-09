package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.input.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import ar.utn.ba.dds.front_tp.services.ColeccionesApiService;
import ar.utn.ba.dds.front_tp.services.HechosApiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

  private final ColeccionesApiService coleccionesApiService;
  private final HechosApiService hechosApiService;
  private static final Logger log = LoggerFactory.getLogger(HomeController.class);

  @GetMapping({"/", "/home"})
  public String mostrarHome(Model model) {

    // 1. Obtenemos las 2 últimas colecciones reales del Backend
    List<ColeccionInputDTO> recientes = coleccionesApiService.obtenerUltimasColecciones(1);

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

    HechoInputDTO ultimoHecho = hechosApiService.obtenerUltimoHecho();

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

  @GetMapping("/privacidad")
  public String mostrarPrivacidad() {
    return "legales/privacidad";
  }

  @GetMapping("/terminos")
  public String mostrarTerminos() {
    return "legales/terminos";
  }

  @GetMapping("/contacto")
  public String mostrarContacto() {
    return "legales/contacto";
  }

  @PostMapping("/contacto")
  public String procesarContacto(@RequestParam String nombre,
                                 @RequestParam String email,
                                 @RequestParam String mensaje,
                                 RedirectAttributes redirectAttributes) {

    // Aquí simularíamos el envío de email o guardado en BD
    log.info("NUEVO MENSAJE DE CONTACTO RECIBIDO:");
    log.info("De: {} ({})", nombre, email);
    log.info("Mensaje: {}", mensaje);

    // Feedback al usuario
    redirectAttributes.addFlashAttribute("mensajeExito", "¡Gracias por contactarnos! Hemos recibido tu mensaje y la sede más cercana te responderá a la brevedad.");

    return "redirect:/contacto";
  }
}
