package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.Utils.JwtUtils;
import ar.utn.ba.dds.front_tp.dto.hechos.CrearHechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.input.SolicitudEliminacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.SoliOutputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.exceptions.DuplicateTitleException;
import ar.utn.ba.dds.front_tp.services.GestionUsuariosApiService;
import ar.utn.ba.dds.front_tp.services.HechosApiService;
import ar.utn.ba.dds.front_tp.services.SolicitudesApiService;
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
import org.springframework.web.bind.annotation.PathVariable;
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
  private final SolicitudesApiService solicitudesApiService;

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
                           RedirectAttributes redirectAttributes,
                           Authentication authentication) {
    AuthResponseDTO token = (AuthResponseDTO) authentication.getDetails();

    if (authentication != null){
      var email = JwtUtils.validarToken(token.getAccessToken());
      hecho.setUsuario(email);
    } else {
      hecho.setUsuario("VISUALIZADOR");
    }
    log.info("usuario en crear hecho: "+ hecho.getUsuario());
    //    log.info("Token recibido del backend de usuarios: {}", token); // 👈
//    log.info("AccessToken: {}", token.getAccessToken()); // 👈
//    log.info("Llegue a crear hechos... creo: " + hecho.getTitulo());

    try {
      CrearHechoDTO payload = new CrearHechoDTO();
      payload.setHecho(hecho);                // Metemos los datos del formulario
      payload.setAccessToken(token.getAccessToken()); // <--- ESTO ES LO QUE FALTABA

      // 3. Llamamos al servicio enviando el PAYLOAD (que tiene token adentro),
      //    y también pasamos el token aparte para el Header HTTP.
      hechosApiService.crearHecho(payload, token.getAccessToken());
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

  @GetMapping("/{id}/detalle")
  public String verDetalleHecho(@PathVariable Long id,
                                Model model,
                                Authentication authentication) {
    try {
      var hecho = hechosApiService.obtenerHecho(id);
      model.addAttribute("hecho", hecho);

      boolean esPropietario = false;

      if (authentication != null && authentication.getDetails() instanceof AuthResponseDTO token) {
        String email = JwtUtils.validarToken(token.getAccessToken());
        if (email != null && hecho.getUsuario() != null) {
          esPropietario = email.equalsIgnoreCase(hecho.getUsuario());
        }
      }

      model.addAttribute("esPropietario", esPropietario);

      return "hecho-detalle";
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      model.addAttribute("errorGlobal", "Ocurrió un error inesperado: " + e.getMessage());
      return "home"; // o la vista que uses para el home
    }
  }

  @GetMapping("/{id}/solicitud-eliminacion")
  public String mostrarFormularioSolicitudEliminacion(@PathVariable Long id,
                                                      Model model,
                                                      Authentication authentication) {
    try {
      var hecho = hechosApiService.obtenerHecho(id);

      SolicitudEliminacionInputDTO solicitud = new SolicitudEliminacionInputDTO();
      solicitud.setTituloHecho(hecho.getTitulo());
      if (authentication != null){
        AuthResponseDTO token = (AuthResponseDTO) authentication.getDetails();
        var email = JwtUtils.validarToken(token.getAccessToken());
        solicitud.setUsuario(email);
      } else {
        solicitud.setUsuario("VISUALIZADOR");
      }

      log.info("USUARIO"+ solicitud.getUsuario());

      boolean esAnonimo = (authentication == null || !authentication.isAuthenticated());

      model.addAttribute("hecho", hecho);
      model.addAttribute("solicitud", solicitud);
      model.addAttribute("esAnonimo", esAnonimo);

      return "solicitud-eliminacion";
    } catch (Exception e) {
      log.error("Error al cargar formulario de solicitud de eliminación para hecho {}: {}", id, e.getMessage(), e);
      model.addAttribute("errorGlobal", "Ocurrió un error al cargar la solicitud de eliminación.");
      return "home";
    }
  }

  @PostMapping("/{id}/solicitud-eliminacion")
  public String enviarSolicitudEliminacion(@PathVariable Long id,
                                           @ModelAttribute("solicitud") SolicitudEliminacionInputDTO solicitud,
                                           BindingResult bindingResult,
                                           Model model,
                                           RedirectAttributes redirectAttributes,
                                           Authentication authentication) {

    boolean esAnonimo = (authentication == null || !authentication.isAuthenticated());

    // Regla: si NO está logueado, justificación mínimo 500 caracteres
    String just = solicitud.getJustificacion() != null ? solicitud.getJustificacion().trim() : "";
    if (esAnonimo && just.length() < 500) {
      bindingResult.rejectValue(
          "justificacion",
          "justificacion.corta",
          "Si no estás logueado, la justificación debe tener al menos 500 caracteres."
      );
    }

    if (bindingResult.hasErrors()) {
      // Volvemos a cargar el hecho para la vista
      var hecho = hechosApiService.obtenerHecho(id);
      model.addAttribute("hecho", hecho);
      model.addAttribute("esAnonimo", esAnonimo);
      return "solicitud-eliminacion";
    }

    try {
      if (authentication != null){
        AuthResponseDTO token = (AuthResponseDTO) authentication.getDetails();
        var email = JwtUtils.validarToken(token.getAccessToken());
        solicitud.setUsuario(email);
      } else {
        solicitud.setUsuario("VISUALIZADOR");
      }

      log.info("USUARIO justito antes de mandar"+ solicitud.getUsuario());

      SoliOutputDTO respuesta = solicitudesApiService.crearSolicitudEliminacion(solicitud);

      redirectAttributes.addFlashAttribute("mensaje",
          "Solicitud enviada correctamente. Código: " + respuesta.getId());
      redirectAttributes.addFlashAttribute("tipoMensaje", "success");

      return "redirect:/hechos/" + id + "/detalle";
    } catch (Exception e) {
      log.error("Error al enviar solicitud de eliminación para hecho {}: {}", id, e.getMessage(), e);
      var hecho = hechosApiService.obtenerHecho(id);
      model.addAttribute("hecho", hecho);
      model.addAttribute("esAnonimo", esAnonimo);
      model.addAttribute("errorGlobal", "Ocurrió un error al enviar la solicitud. Intenta nuevamente.");
      return "solicitud-eliminacion";
    }
  }

  @GetMapping("/mis-hechos")
  public String verMisHechos(Model model,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
    if (authentication == null || !(authentication.getDetails() instanceof AuthResponseDTO token)) {
      redirectAttributes.addFlashAttribute("errorLogin",
          "Para ver tus hechos debes iniciar sesión.");
      return "redirect:/auth";
    }

    try {
      // Email del usuario desde el token JWT
      String email = JwtUtils.validarToken(token.getAccessToken());

      // Llamamos al backend para traer los hechos del usuario
      var hechosUsuario = hechosApiService.obtenerHechosUsuario(email);

      model.addAttribute("hechos", hechosUsuario);
      model.addAttribute("usuarioEmail", email);

      return "mis-hechos";  // => templates/mis-hechos.html
    } catch (Exception e) {
      log.error("Error al obtener hechos del usuario", e);
      model.addAttribute("errorGlobal", "Ocurrió un error al obtener tus hechos. Intenta más tarde.");
      return "mis-hechos"; // Mostramos la vista igual pero vacía
    }
  }

  @GetMapping("/{id}/editar")
  public String editarHecho(@PathVariable Long id, Model model, Authentication authentication) {

    HechoDTO hecho = hechosApiService.obtenerHecho(id);

    model.addAttribute("hecho", hecho);
    return "editar-hecho";
  }
  @PostMapping("/{id}/editar")
  public String subirHechoEditado(@PathVariable Long id, Model model, Authentication authentication) {


  }
