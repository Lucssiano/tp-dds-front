package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionDTO;
import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.dto.admin.DashboardSummaryDTO;
import ar.utn.ba.dds.front_tp.services.ColeccionesApiService;
import ar.utn.ba.dds.front_tp.services.DashboardApiService;
import ar.utn.ba.dds.front_tp.services.RevisionesApiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

  private final ColeccionesApiService coleccionesApiService;
  private final DashboardApiService dashboardApiService;
  private final RevisionesApiService revisionesApiService;
  private static final Logger log = LoggerFactory.getLogger(AdminController.class);

  @GetMapping("/colecciones")
  public String gestionarColecciones(Model model) {
    List<ColeccionDTO> colecciones = coleccionesApiService.obtenerColecciones();
    model.addAttribute("colecciones", colecciones);
    if (!model.containsAttribute("coleccionNueva")) {
      model.addAttribute("coleccionNueva", new ColeccionInputDTO());
    }
    return "admin-colecciones";
  }
  @GetMapping("/colecciones/crear")
  public String mostrarFormularioCreacion(Model model) {
    model.addAttribute("coleccion", new ColeccionInputDTO());
    return "admin-crear-coleccion";
  }

  @PostMapping("/colecciones/crear")
  public String crearColeccion(@ModelAttribute("coleccionNueva") ColeccionInputDTO coleccionInput,
                               Authentication authentication, // Inyectamos Authentication
                               RedirectAttributes redirectAttributes) {

    // Obtenemos el DTO de los "detalles" del objeto Authentication
    log.info("😎Llegamos as post de crear coleccion: "+ coleccionInput.getTitulo());
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    if (authData == null || authData.getAccessToken() == null) {
      redirectAttributes.addFlashAttribute("error", "Tu sesión ha expirado.");
      return "redirect:/auth/login";
    }

    try {
      coleccionesApiService.crearColeccion(coleccionInput, authData.getAccessToken());
      redirectAttributes.addFlashAttribute("mensaje", "¡Colección creada exitosamente!");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al crear la colección: " + e.getMessage());
    }
    return "redirect:/admin/colecciones";
  }

  @GetMapping("/colecciones/eliminar/{id}")
  public String eliminarColeccion(@PathVariable Long id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {

    // 1. Obtener token de sesión
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    if (authData == null || authData.getAccessToken() == null) {
      return "redirect:/auth/login";
    }

    try {
      // 2. Llamar al servicio para borrar
      coleccionesApiService.eliminarColeccion(id, authData.getAccessToken());
      redirectAttributes.addFlashAttribute("mensaje", "Colección eliminada correctamente.");

    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "No se pudo eliminar la colección: " + e.getMessage());
    }

    // 3. Redirigir a la lista
    return "redirect:/admin/colecciones";
  }

  // GET: Mostrar el formulario de "crear" coleccion lleno para modificar una colección.
  @GetMapping("/colecciones/editar/{id}")
  public String mostrarFormularioEdicion(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
    try {
      ColeccionDTO existente = coleccionesApiService.obtenerColeccionPorId(id);

      ColeccionInputDTO form = new ColeccionInputDTO();
      form.setTitulo(existente.getTitulo());
      form.setDescripcion(existente.getDescripcion());
      form.setAlgoritmoConsenso(existente.getAlgoritmoConsenso());
      form.setFuentes(existente.getFuentes());
      form.setCriteriosDePertenencias(existente.getCriteriosDePertenencias());

      model.addAttribute("coleccion", form);
      model.addAttribute("idColeccion", id);

      return "admin-editar-coleccion";
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
      return "redirect:/admin/colecciones";
    }
  }

  // POST: Guardar los cambios luego de modificar una colección.
  @PostMapping("/colecciones/editar/{id}")
  public String procesarEdicion(@PathVariable Long id,
                                @ModelAttribute("coleccion") ColeccionInputDTO coleccionInput,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();

    try {
      coleccionesApiService.modificarColeccion(id, coleccionInput, authData.getAccessToken());
      redirectAttributes.addFlashAttribute("mensaje", "Colección modificada con éxito.");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al modificar: " + e.getMessage());
      // Si falla, podríamos volver al formulario, pero por simpleza redirigimos a la lista
    }
    return "redirect:/admin/colecciones";
  }

  @GetMapping("/dashboard")
  public String mostrarDashboard(Model model, Authentication authentication) { // Ya no necesitamos HttpSession
    log.info("Entre a mostrar dashboard" + authentication.getCredentials());
    // Obtenemos el DTO de los "detalles" del objeto Authentication
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();

    // Llamo al servicio para obtener los datos del resumen
    DashboardSummaryDTO summary = dashboardApiService.getSummary(authData.getAccessToken());

    // Paso los datos al modelo
    model.addAttribute("summary", summary);

    return "admin-dashboard";
  }

  @GetMapping("/revisiones")
  public String gestionarRevisiones(Model model, Authentication authentication) {
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    String token = authData.getAccessToken();

    // Cargar listas
    var hechos = revisionesApiService.obtenerHechosPendientes(token);
    var solicitudes = revisionesApiService.obtenerSolicitudesPendientes(token);

    model.addAttribute("hechosPendientes", hechos);
    model.addAttribute("solicitudesPendientes", solicitudes);

    return "admin-revisiones";
  }
  // Acciones sobre Hechos (Aprobar/Rechazar)
  @PostMapping("/revisiones/hechos/{id}/{accion}")
  public String accionesHecho(@PathVariable Long id,
                              @PathVariable String accion,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();

    try {
      if ("aprobar".equals(accion)) {
        revisionesApiService.aprobarHecho(id, authData.getAccessToken());
        redirectAttributes.addFlashAttribute("mensaje", "Hecho aprobado correctamente.");
      } else if ("rechazar".equals(accion)) {
        revisionesApiService.rechazarHecho(id, authData.getAccessToken());
        redirectAttributes.addFlashAttribute("mensaje", "Hecho rechazado correctamente.");
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al procesar el hecho: " + e.getMessage());
    }
    return "redirect:/admin/revisiones";
  }
  // TODO: no funcionan los botones aeptar y rechazar, queda pendiente de solucionar.
  // Acciones sobre Solicitudes (Aceptar eliminación / Rechazar solicitud)
  @PostMapping("/revisiones/solicitudes/{id}/{accion}")
  public String accionesSolicitud(@PathVariable Long id,
                                  @PathVariable String accion,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();

    try {
      if ("aceptar".equals(accion)) { // Eliminar el hecho reportado
        revisionesApiService.aceptarSolicitud(id, authData.getAccessToken());
        redirectAttributes.addFlashAttribute("mensaje", "Solicitud aceptada y hecho eliminado.");
      } else if ("rechazar".equals(accion)) { // Descartar la solicitud
        revisionesApiService.rechazarSolicitud(id, authData.getAccessToken());
        redirectAttributes.addFlashAttribute("mensaje", "Solicitud descartada.");
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al procesar la solicitud: " + e.getMessage());
    }
    return "redirect:/admin/revisiones";
  }

  // TODO: Falta el método de editar (GET) mencionado en el HTML, necesitarías una vista nueva para editar el hecho.
  @GetMapping("/revisiones/hechos/{id}/editar")
  public String editarHecho(@PathVariable Long id, Model model) {
    // Lógica para buscar el hecho individual y mostrar formulario de edición
    return "admin-editar-hecho"; // Placeholder
  }

}

