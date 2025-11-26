package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionDTO;
import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.input.SolicitudModificacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.dto.admin.DashboardSummaryDTO;
import ar.utn.ba.dds.front_tp.exceptions.api.AutenticationException;
import ar.utn.ba.dds.front_tp.exceptions.api.AuthorizationException;
import ar.utn.ba.dds.front_tp.exceptions.api.GeneralApiException;
import ar.utn.ba.dds.front_tp.exceptions.api.InternalServerErrorException;
import ar.utn.ba.dds.front_tp.exceptions.api.ResourceNotFoundException;
import ar.utn.ba.dds.front_tp.exceptions.api.ValidationException;
import ar.utn.ba.dds.front_tp.services.ColeccionesApiService;
import ar.utn.ba.dds.front_tp.services.DashboardApiService;
import ar.utn.ba.dds.front_tp.services.FuentesApiService;
import ar.utn.ba.dds.front_tp.services.HechosApiService;
import ar.utn.ba.dds.front_tp.services.RevisionesApiService;
import ar.utn.ba.dds.front_tp.services.SolicitudesModificacionApiService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
  private final FuentesApiService fuentesApiService;
  private final ColeccionesApiService coleccionesApiService;
  private final DashboardApiService dashboardApiService;
  private final RevisionesApiService revisionesApiService;
  private final HechosApiService hechosApiService;
  private final SolicitudesModificacionApiService solicitudesModificacionApiService;
  private static final Logger log = LoggerFactory.getLogger(AdminController.class);

  @GetMapping("/colecciones")
  public String gestionarColecciones(Model model) {
    List<ColeccionDTO> colecciones = coleccionesApiService.obtenerColecciones();
    model.addAttribute("colecciones", colecciones);
    return "admin-colecciones";
  }
  @GetMapping("/colecciones/crear")
  public String mostrarFormularioCreacion(Model model) {
    model.addAttribute("coleccion", new ColeccionInputDTO());
    List<String> fuentesDisponibles = fuentesApiService.obtenerFuentes().getFuentes();
    model.addAttribute("fuentesDisponibles", fuentesDisponibles);
    log.info("Fuentes disponibles: " + fuentesDisponibles);
    return "admin-crear-coleccion";
  }

//  @PostMapping("/colecciones/crear")
//  public String crearColeccion(@ModelAttribute("coleccionNueva") ColeccionInputDTO coleccionInput,
//                               Authentication authentication, // Inyectamos Authentication
//                               RedirectAttributes redirectAttributes) {
//
//    // Obtenemos el DTO de los "detalles" del objeto Authentication
//    log.info("😎Llegamos as post de crear coleccion: "+ coleccionInput.getTitulo());
//    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
//    if (authData == null || authData.getAccessToken() == null) {
//      redirectAttributes.addFlashAttribute("error", "Tu sesión ha expirado.");
//      return "redirect:/auth/login";
//    }
//
//    try {
//      coleccionesApiService.crearColeccion(coleccionInput, authData.getAccessToken());
//      redirectAttributes.addFlashAttribute("mensaje", "¡Colección creada exitosamente!");
//    } catch (Exception e) {
//      redirectAttributes.addFlashAttribute("error", "Error al crear la colección: " + e.getMessage());
//    }
//    return "redirect:/admin/colecciones";
//  }

  @PostMapping("/colecciones/crear")
  public String crearColeccion(@ModelAttribute("coleccion") ColeccionInputDTO coleccionInput,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {

    log.info("😎 Llegamos al post de crear coleccion: "+ coleccionInput.getTitulo());

    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    if (authData == null || authData.getAccessToken() == null) {
      redirectAttributes.addFlashAttribute("error", "Tu sesión ha expirado.");
      return "redirect:/auth/login";
    }

    try {
      coleccionesApiService.crearColeccion(coleccionInput, authData.getAccessToken()).block();
      redirectAttributes.addFlashAttribute("mensaje", "¡Colección creada exitosamente!");
      return "redirect:/admin/colecciones";
    }
    // 1. Errores de Formulario (400/422)
    catch (ValidationException ex) {
      model.addAttribute("errors", ex.getApiError().fields());
      model.addAttribute("coleccion", coleccionInput);
      model.addAttribute("fuentesDisponibles", fuentesApiService.obtenerFuentes().getFuentes());
      return "admin-crear-coleccion";
    }
    // 2. Errores de Autenticación (401)
    catch (AutenticationException ex) {
      redirectAttributes.addFlashAttribute("error", "Tu sesión ha expirado. Por favor, vuelve a ingresar.");
      return "redirect:/auth/login";
    }
    // 3. Errores de Autorización (403)
    catch (AuthorizationException ex) {
      redirectAttributes.addFlashAttribute("error", "Acceso denegado: No tienes permisos.");
      return "redirect:/error/403";
    }
    // 4. Errores de Recurso No Encontrado (404)
    catch (ResourceNotFoundException ex) {
      redirectAttributes.addFlashAttribute("error", "El recurso solicitado no fue encontrado.");
      return "redirect:/error/404";
    }
    // 5. Errores de Servidor (5xx)
    catch (InternalServerErrorException ex) {
      // Es mejor evitar mostrar el mensaje técnico 5xx al usuario final
      redirectAttributes.addFlashAttribute("error", "Error del sistema. Intente nuevamente.");
      return "redirect:/admin/colecciones";
    }
    // 6. Errores Generales de API (Fallback 4xx no mapeado, ej. 409 Conflict)
    catch (GeneralApiException ex) {
      String message = ex.getApiError() != null ? ex.getApiError().message() : "Error inesperado de API.";
      redirectAttributes.addFlashAttribute("error", "Error de la API: " + message);
      return "redirect:/admin/colecciones";
    }
    // 7. Fallback de Java (Network, I/O, Error de Bloqueo .block(), etc.)
    catch (Exception ex) {
      // Este catch atrapa cualquier fallo de bajo nivel que no provenga del flujo Mono.error()
      log.error("Fallo inesperado de bajo nivel: {}", ex.getMessage());
      redirectAttributes.addFlashAttribute("error", "Fallo de comunicación: " + ex.getMessage());
      return "redirect:/admin/colecciones";
    }
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

  @GetMapping("/colecciones/editar/{id}")
  public String mostrarFormularioEdicion(@PathVariable Long id,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
    try {
      ColeccionDTO existente = coleccionesApiService.obtenerColeccionPorId(id);

      ColeccionInputDTO form = new ColeccionInputDTO();
      form.setTitulo(existente.getTitulo());
      form.setDescripcion(existente.getDescripcion());
      form.setAlgoritmoConsenso(existente.getAlgoritmoConsenso());

      // Fuentes ya seleccionadas (las que vienen del backend)
      form.setFuentes(
          existente.getFuentes() != null
              ? new ArrayList<>(existente.getFuentes())
              : new ArrayList<>()
      );

      // Criterios existentes (incluye tipoCriterio + parametros)
      if (existente.getCriteriosDePertenencias() != null) {
        existente.getCriteriosDePertenencias().forEach(c -> {
          if (c.getParametros() == null) {
            c.setParametros(new HashMap<>()); // por si acaso
          }
        });
        form.setCriteriosDePertenencias(new ArrayList<>(existente.getCriteriosDePertenencias()));
      } else {
        form.setCriteriosDePertenencias(new ArrayList<>());
      }

      model.addAttribute("coleccion", form);
      model.addAttribute("idColeccion", id);

      // Fuentes disponibles para checkboxes
      List<String> fuentesDisponibles = fuentesApiService.obtenerFuentes().getFuentes();
      model.addAttribute("fuentesDisponibles", fuentesDisponibles);

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
    var solicitudesModificacion = solicitudesModificacionApiService.obtenerSolicitudesModificacionPendientes();

    model.addAttribute("hechosPendientes", hechos);
    model.addAttribute("solicitudesPendientes", solicitudes);
    model.addAttribute("modificacionesPendientes", solicitudesModificacion);

    return "admin-revisiones";
  }

  @GetMapping("/revisiones/hechos/{id}/detalle")
  public String verDetalleHecho(@PathVariable Long id,
                                Model model,
                                RedirectAttributes redirectAttributes) {
    try {
      // Traer el hecho individual
      var hecho = hechosApiService.obtenerHecho(id);

      model.addAttribute("hecho", hecho);
      model.addAttribute("modoEdicion", false);

      return "admin-detalle-hecho";

    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "No se pudo cargar el hecho.");
      return "redirect:/admin/revisiones";
    }
  }

  @GetMapping("/revisiones/hechos/{id}/editar")
  public String editarHecho(@PathVariable Long id,
                            Model model,
                            RedirectAttributes redirectAttributes) {
    try {
      // Traer el hecho individual
      var hecho = hechosApiService.obtenerHecho(id);

      model.addAttribute("hecho", hecho);
      model.addAttribute("modoEdicion", true);

      return "admin-detalle-hecho";

    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "No se pudo cargar el hecho.");
      return "redirect:/admin/revisiones";
    }
  }

  @PostMapping("/revisiones/hechos/{id}/editar")
  public String editarHecho(@PathVariable Long id,
                            @ModelAttribute("hecho") HechoDTO hechoDTO,
                            RedirectAttributes redirectAttributes) {
    try {
      hechosApiService.editarHecho(id, hechoDTO);
      redirectAttributes.addFlashAttribute("mensaje", "Hecho editado con éxito.");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al editar: " + e.getMessage());
    }
    return "redirect:/admin/revisiones";
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

  @GetMapping("/revisiones/modificaciones/{id}/detalle")
  public String verDetalleModificacion(@PathVariable Long id,
                                       @ModelAttribute("soliModificacion") SolicitudModificacionInputDTO solicitudModificacion,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
    try {
      model.addAttribute("solicitudModificacion", solicitudModificacion);

      return "admin-detalle-modificacion";

    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "No se pudo cargar el hecho modificado.");
      return "redirect:/admin/revisiones";
    }
  }

  @PostMapping("/revisiones/modificaciones/{id}/{accion}")
  public String accionesSolicitudModificacion(@PathVariable Long id,
                              @PathVariable String accion,
                              RedirectAttributes redirectAttributes) {
    try {
      if ("aprobar".equals(accion)) {
        solicitudesModificacionApiService.aceptarSolicitudModificacion(id);
        redirectAttributes.addFlashAttribute("mensaje", "Hecho aprobado correctamente.");
      } else if ("rechazar".equals(accion)) {
        solicitudesModificacionApiService.rechazarSolicitudModificacion(id);
        redirectAttributes.addFlashAttribute("mensaje", "Hecho rechazado correctamente.");
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al procesar el hecho: " + e.getMessage());
    }
    return "redirect:/admin/revisiones";
  }

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

  @GetMapping("/fuentes")
  public String mostrarFuentes(Model model, Authentication authentication) {

    List<String> fuentesDisponibles = fuentesApiService.obtenerFuentes().getFuentes();
    model.addAttribute("fuentesDisponibles", fuentesDisponibles);

    return "admin-fuentes";
  }
}

