package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.exceptions.DuplicateTitleException;
import ar.utn.ba.dds.front_tp.services.GestionUsuariosApiService;
import ar.utn.ba.dds.front_tp.services.HechosApiService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import java.util.List;

@Controller
@RequestMapping("/hechos")
@RequiredArgsConstructor
public class HechosController {
  private static final Logger log = LoggerFactory.getLogger(HechosController.class);
  private final HechosApiService hechosApiService;
  @Autowired
  private HttpSession session;

  @GetMapping("/mapa")
  public String mostrarMapa(Model model) {
    log.info(">>> Entrando a /hechos/mapa");
    try {
      List<HechoDTO> hechos = hechosApiService.obtenerHechos();
      log.info("Cantidad de hechos recibidos: {}", hechos.size());
      model.addAttribute("hechos", hechos);
    } catch (Exception e) {
      log.error("Error al obtener hechos", e);
      model.addAttribute("error", "Error al obtener hechos: " + e.getMessage());
    }
    return "mapa";
  }
  @GetMapping("/subir-hecho")
  //@PreAuthorize("hasAnyRole('ADMIN', 'CONTRIBUYENTE')")
  public String subirHecho(Model model){
    model.addAttribute("hecho", new HechoDTO());
    return "subir-hecho";
  }

  @PostMapping("/crear-hecho")
  @PreAuthorize("hasAnyRole('ADMIN', 'CONTRIBUYENTE')")
  public String crearHecho(@ModelAttribute("hecho") HechoDTO hecho,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {

    AuthResponseDTO token = (AuthResponseDTO) session.getAttribute("AUTH_DATA");

    if (token == null) {
      redirectAttributes.addFlashAttribute("errorLogin", "Tu sesión ha expirado. Por favor, inicia sesión de nuevo.");
      return "redirect:/auth/login";
    }
    try {
      HechoDTO hechoCreado = hechosApiService.crearHecho(hecho, token.getAccessToken());
      redirectAttributes.addFlashAttribute("mensaje", "Hecho creado exitosamente");
      redirectAttributes.addFlashAttribute("tipoMensaje", "success");
      return "redirect:/hechos/" + hechoCreado.getTitulo();
    } catch (DuplicateTitleException ex) {
      // Duplicidad: Para un campo de tu DTO (ej. si el título de un hecho debe ser único)
      bindingResult.rejectValue("titulo", "error.titulo.duplicado", ex.getMessage());
      model.addAttribute("hecho", hecho); // Vuelve a cargar el DTO para que el usuario no pierda los datos
      model.addAttribute("errorGlobal", "El título de Hecho ya existe. Por favor, elige otro.");
      return "crear-hecho"; // Retorna a la vista del formulario
    } catch (RuntimeException e) {
      // Errores de API/Comunicación: Fallo al consumir el servicio REST o error 5xx del backend.
      log.error("Error al crear hecho por falla de servicio", e);
      model.addAttribute("errorGlobal", "No se pudo comunicar con el servicio. Inténtalo más tarde.");
      model.addAttribute("hecho", hecho);
      return "crear-hecho";
    } catch (Exception e) {
      // Fallback: Error inesperado que no manejamos.
      log.error("Error inesperado al crear hecho", e);
      model.addAttribute("errorGlobal", "Ocurrió un error inesperado: " + e.getMessage());
      model.addAttribute("hecho", hecho);
      return "crear-hecho";
    }
  }
}
