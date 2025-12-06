package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.admin.ActividadDTO;
import ar.utn.ba.dds.front_tp.dto.admin.CategoriaDTO;
import ar.utn.ba.dds.front_tp.dto.admin.ColeccionEstadisticaDTO;
import ar.utn.ba.dds.front_tp.dto.input.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.FuenteInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.input.SolicitudModificacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.ColeccionOutputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.dto.admin.DashboardSummaryDTO;
import ar.utn.ba.dds.front_tp.exceptions.api.ValidationException;
import ar.utn.ba.dds.front_tp.services.*;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

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
  private final EstadisticasApiService estadisticasApiService;
  private static final Logger log = LoggerFactory.getLogger(AdminController.class);

  @GetMapping("/colecciones")
  public String gestionarColecciones(Model model) {
    List<ColeccionInputDTO> colecciones = coleccionesApiService.obtenerColecciones();
    model.addAttribute("colecciones", colecciones);
    return "admin-colecciones";
  }
  @GetMapping("/colecciones/crear")
  public String mostrarFormularioCreacion(Model model) {
    model.addAttribute("coleccion", new ColeccionOutputDTO());
    List<FuenteInputDTO> fuentesDisponibles = fuentesApiService.obtenerFuentes();
    model.addAttribute("fuentesDisponibles", fuentesDisponibles);
    log.info("Fuentes disponibles: " + fuentesDisponibles);
    return "admin-crear-coleccion";
  }

//  @PostMapping("/colecciones/crear")
//  public String crearColeccion(@ModelAttribute("coleccionNueva") ColeccionInputDTO coleccionInput,
//                               Authentication authentication, // Inyectamos Authentication
//                               RedirectAttributes redirectAttributes) {
//
//    // Obtenemos el ColeccionOutputDTO de los "detalles" del objeto Authentication
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
  public String crearColeccion(@ModelAttribute("coleccion") ColeccionOutputDTO coleccionOutputDTO,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {

    log.info("😎 Llegamos al post de crear coleccion: "+ coleccionOutputDTO.getTitulo());

    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    if (authData == null || authData.getAccessToken() == null) {
      redirectAttributes.addFlashAttribute("error", "Tu sesión ha expirado.");
      return "redirect:/auth/login";
    }

    try {
      coleccionesApiService.crearColeccion(coleccionOutputDTO, authData.getAccessToken()).block();
      redirectAttributes.addFlashAttribute("mensaje", "¡Colección creada exitosamente!");
      return "redirect:/admin/colecciones";
    }
    // Errores de Formulario (400/422)
    catch (ValidationException ex) {
      model.addAttribute("errors", ex.getApiError().fields());
      model.addAttribute("coleccion", coleccionOutputDTO);
      model.addAttribute("fuentesDisponibles", fuentesApiService.obtenerFuentes());
      return "admin-crear-coleccion";
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
      ColeccionInputDTO existente = coleccionesApiService.obtenerColeccionPorId(id);

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
      List<FuenteInputDTO> fuentesDisponibles = fuentesApiService.obtenerFuentes();
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
  public String mostrarDashboard(Model model, Authentication authentication) {
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    String token = authData.getAccessToken();

    DashboardSummaryDTO summary;
    List<ActividadDTO> actividad;

    // 1. CARGA DEL SUMMARY (Protegida)
    try {
      summary = dashboardApiService.getSummary(token);
    } catch (Exception e) {
      // Si falla, inicializamos el ColeccionOutputDTO con ceros para EVITAR el SpelEvaluationException
      log.error("Fallo al obtener resumen del dashboard: {}", e.getMessage());
      // Asumiendo el constructor (hechosPendientes, solicitudesEliminacion, solicitudesModificacion, coleccionesActivas)
      summary = DashboardSummaryDTO.builder()
          .hechosPendientes(0L)
          .solicitudesEliminacion(0L)
          .solicitudesModificacion(0L)
          .coleccionesActivas(0L)
          .build();
      model.addAttribute("error", "Fallo la carga de estadísticas. Intente de nuevo.");
    }

    model.addAttribute("summary", summary);

    // 2. CARGA DE ACTIVIDAD RECIENTE (Protegida)
    try {
      actividad = dashboardApiService.obtenerActividadReciente(token);
      model.addAttribute("actividadReciente", actividad);
    } catch (Exception e) {
      log.error("Error al cargar Actividad Reciente: {}", e.getMessage());
      // Si falla, pasamos una lista vacía para que Thymeleaf no explote en el th:each
      model.addAttribute("actividadReciente", Collections.emptyList());
    }

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
                            @ModelAttribute("hecho") @Valid HechoInputDTO hechoInputDTO, // Spring ya lo mete al modelo automáticamente
                            Model model, // Usamos Model, no RedirectAttributes para errores
                            RedirectAttributes redirectAttributes) {
    try {
      hechosApiService.editarHecho(id, hechoInputDTO);

      // ÉXITO -> REDIRECT
      redirectAttributes.addFlashAttribute("mensaje", "Hecho editado con éxito.");
      return "redirect:/admin/revisiones/hechos/" + id + "/detalle";

    } catch (ValidationException ex) {
      // ERROR -> RENDERIZAR VISTA (No redirect)

      // 1. Agregamos el hecho y el modo de edición
      model.addAttribute("hecho", hechoInputDTO);
      model.addAttribute("modoEdicion", true);

      // 2. Pasamos los errores
      model.addAttribute("errors", ex.getApiError().fields());

      // 3. (Opcional) Si tienes desplegables, cárgalos de nuevo aquí
      // model.addAttribute("categorias", servicio.obtenerCategorias());

      // 4. Devolvemos el nombre del HTML del formulario de edición
      return "admin-detalle-hecho";
    }
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

    List<FuenteInputDTO> fuentesDisponibles = fuentesApiService.obtenerFuentes();
    model.addAttribute("fuentesDisponibles", fuentesDisponibles);

    return "admin-fuentes";
  }

  @PostMapping("/importar-hechos")
  public String importarHechos(@RequestParam("archivoCsv") MultipartFile file,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();

    try {
      // Llamamos al servicio (código abajo)
      dashboardApiService.importarHechos(file, authData.getAccessToken());
      redirectAttributes.addFlashAttribute("mensaje", "Archivo enviado a procesar correctamente.");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al subir archivo: " + e.getMessage());
    }

    return "redirect:/admin/dashboard";
  }
    @GetMapping("/estadisticas")
    public String mostrarEstadisticas(
            @RequestParam(value = "top", required = false) Boolean top,
            @RequestParam(value = "categorias", required = false) List<String> categorias,
            Model model) {

        log.info("➡️ Iniciando /estadisticas");
        log.info("➡️ Parametro top = {}", top);
        log.info("➡️ Parametro categorias = {}", categorias);

        // 1. Llamo al backend
        List<CategoriaDTO> resultado = estadisticasApiService.obtenerCategorias(categorias, top);

        log.info("✔️ Backend respondió {} categorías", resultado.size());
        CategoriaDTO categoriaMax = estadisticasApiService.obtenerCategorias(categorias, true).get(0);

        List<ColeccionEstadisticaDTO> resultadoColecciones = estadisticasApiService.obtenerColecciones(List.of());



        // 2. Cargo resultados en el model
        model.addAttribute("categorias", resultado);
        model.addAttribute("categoriaMaxima",categoriaMax);
        model.addAttribute("colecciones",resultadoColecciones);

        // 3. Lista de nombres
        List<String> nombres = resultado.stream()
                .map(CategoriaDTO::getCategoria)
                .toList();

        model.addAttribute("nombresCategorias", nombres);
        log.info("✔️ nombresCategorias = {}", nombres);

        // 4. Mandar estado actual del filtro (IMPORTANTE PARA EVITAR EL ERROR)
        model.addAttribute("filtroCategorias", categorias);
        model.addAttribute("filtroTop", top);

        // FIX: atributo que Thymeleaf necesita para el selected
        model.addAttribute("categoriaSeleccionada",
                categorias != null ? categorias : List.of());

        log.info("✔️ categoriaSeleccionada = {}", categorias);

        return "admin-estadisticas";
    }





//    @GetMapping("/estadisticas/csv")
//    public ResponseEntity<byte[]> exportarEstadisticasCSV() {
//        // Datos hardcodeados de ejemplo
//        String csvContent = "Provincia,Categoría,Cantidad\n" +
//                "Buenos Aires,Terremotos,15\n" +
//                "Cordoba,Inundaciones,8\n" +
//                "Santa Fe,Incendios,12\n";
//
//        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"estadisticas.csv\"")
//                .contentType(MediaType.parseMediaType("text/csv"))
//                .body(bytes);
//    }

}

