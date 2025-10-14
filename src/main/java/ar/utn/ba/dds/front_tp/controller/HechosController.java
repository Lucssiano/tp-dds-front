package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.exceptions.DuplicateTitleException;
import ar.utn.ba.dds.front_tp.services.GestionUsuariosApiService;
import ar.utn.ba.dds.front_tp.services.HechosApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

  // Inyectamos el conversor de JSON
  private final ObjectMapper objectMapper;
  @Autowired
  private HttpSession session;

  @GetMapping("/mapa")
  public String mostrarMapa(
    @RequestParam(required = false, defaultValue = "CURADO") String modo,
    @RequestParam(required = false, name = "fechaAcontecimientoDesde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
    @RequestParam(required = false, name = "fechaAcontecimientoHasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
    Model model) {
    try {
      List<HechoDTO> hechos = hechosApiService.obtenerHechos(modo, fechaDesde, fechaHasta);
      log.info("Cantidad de hechos recibidos: {}", hechos.size());

      // Convertimos la lista a un String JSON
      String hechosJson = objectMapper.writeValueAsString(hechos);

      // Pasamos el STRING JSON al modelo
      model.addAttribute("hechosJson", hechosJson);
      model.addAttribute("modoActual", modo);
      model.addAttribute("fechaDesde", fechaDesde != null ? fechaDesde.toString() : "");
      model.addAttribute("fechaHasta", fechaHasta != null ? fechaHasta.toString() : "");
    } catch (Exception e) {
      log.error("Error al obtener hechos o al convertirlos a JSON", e);
      model.addAttribute("hechosJson", "[]"); // Pasamos un array vacío en caso de error
      model.addAttribute("modoActual", modo); // Pasamos un array vacío en caso de error
    }
    return "mapa";
  }
  @GetMapping("/subir-hecho")
  public String subirHecho(Model model){
    model.addAttribute("hecho", new HechoOutputDTO());
    return "subir-hecho";
  }


  @PostMapping("/crear-hecho")
  //@PreAuthorize("hasAnyRole('ADMIN', 'CONTRIBUYENTE')")
  public String crearHecho(@ModelAttribute("hecho") HechoOutputDTO  hecho,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {

    AuthResponseDTO token = (AuthResponseDTO) session.getAttribute("AUTH_DATA");
    log.info("Token recibido del backend de usuarios: {}", token); // 👈
    log.info("AccessToken: {}", token.getAccessToken()); // 👈
    log.info("Llegue a crear hechos... creo: " + hecho.getTitulo());
    if (token == null) {
      redirectAttributes.addFlashAttribute("errorLogin", "Tu sesión ha expirado. Por favor, inicia sesión de nuevo.");
      return "redirect:/auth/login";
    }
    try {
      HechoOutputDTO hechoCreado = hechosApiService.crearHecho(hecho, token.getAccessToken());
      redirectAttributes.addFlashAttribute("mensaje", "Hecho creado exitosamente");
      redirectAttributes.addFlashAttribute("tipoMensaje", "success");
      return "redirect:/home";
    } catch (DuplicateTitleException ex) {
      // Duplicidad: Para un campo de tu DTO (ej. si el título de un hecho debe ser único)
      bindingResult.rejectValue("titulo", "error.titulo.duplicado", ex.getMessage());
      model.addAttribute("hecho", hecho); // Vuelve a cargar el DTO para que el usuario no pierda los datos
      model.addAttribute("errorGlobal", "El título de Hecho ya existe. Por favor, elige otro.");
      return "subir-hecho"; // Retorna a la vista del formulario
    } catch (RuntimeException e) {
      // Errores de API/Comunicación: Fallo al consumir el servicio REST o error 5xx del backend.
      log.error("Error al crear hecho por falla de servicio", e);
      model.addAttribute("errorGlobal", "No se pudo comunicar con el servicio. Inténtalo más tarde.");
      model.addAttribute("hecho", hecho);
      return "subir-hecho";
    } catch (Exception e) {
      // Fallback: Error inesperado que no manejamos.
      log.error("Error inesperado al crear hecho", e);
      model.addAttribute("errorGlobal", "Ocurrió un error inesperado: " + e.getMessage());
      model.addAttribute("hecho", hecho);
      return "subir-hecho";
    }
  }
}
