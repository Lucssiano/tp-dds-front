package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.TipoCriterio;
import ar.utn.ba.dds.front_tp.dto.admin.ActividadDTO;
import ar.utn.ba.dds.front_tp.dto.admin.CategoriaEstadisticaDTO;
import ar.utn.ba.dds.front_tp.dto.admin.ColeccionEstadisticaDTO;
import ar.utn.ba.dds.front_tp.dto.editar.EditarHechoDTO;
import ar.utn.ba.dds.front_tp.dto.input.ApiError;
import ar.utn.ba.dds.front_tp.dto.input.CategoriaInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.FuenteInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.SolicitudModificacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.SolicitudEliminacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.ColeccionOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.CriterioDePertenenciaOutputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.dto.admin.DashboardSummaryDTO;
import ar.utn.ba.dds.front_tp.exceptions.api.ApiException;
import ar.utn.ba.dds.front_tp.mappers.HechoMapper;
import ar.utn.ba.dds.front_tp.services.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
  private final SolicitudesEliminacionApiService solicitudesEliminacionApiService;
  private final EstadisticasApiService estadisticasApiService;
  private final UploadFileService imagenesService;
  private final HechoMapper hechoMapper;

  private static final Logger log = LoggerFactory.getLogger(AdminController.class);

  // ========================================================================================
  // MÉTODOS HELPER (PRIVADOS) - REUTILIZACIÓN DE LÓGICA DE CARGA
  // ========================================================================================

  private void cargarCategoriasEnModelo(Model model) {
    try {
      List<CategoriaInputDTO> categorias = this.hechosApiService.obtenerCategorias();
      model.addAttribute("categoriasDisponibles", categorias);
    } catch (Exception ex) {
      if (ex instanceof ApiException) {
        log.warn("⚠️ No se cargaron las categorías. Causa: {}", ex.getMessage());
      } else {
        log.error("🔥 BUG: Falló la carga de categorías.", ex);
      }
      // Lista vacía para evitar errores en la vista
      model.addAttribute("categoriasDisponibles", new ArrayList<CategoriaInputDTO>());
      model.addAttribute("warningCategorias", "No se pudieron cargar las categorías existentes.");
    }
  }

  private void cargarFuentesEnModelo(Model model) {
    try {
      List<FuenteInputDTO> fuentes = this.fuentesApiService.obtenerFuentes();
      // Usamos el nombre 'fuentesDisponibles' que es común en tus vistas de edición
      model.addAttribute("fuentesDisponibles", fuentes);
    } catch (Exception ex) {
      if (ex instanceof ApiException) {
        log.warn("⚠️ No se cargaron las fuentes. Causa: {}", ex.getMessage());
      } else {
        log.error("🔥 BUG: Falló la carga de fuentes.", ex);
      }
      model.addAttribute("fuentesDisponibles", new ArrayList<FuenteInputDTO>());
      model.addAttribute("warningFuentes", "No se pudieron cargar las fuentes existentes.");
    }
  }

  // Método auxiliar para validar lógica compleja de criterios
  private void validarCriterios(ColeccionOutputDTO coleccion, BindingResult bindingResult) {
    if (coleccion.getCriteriosDePertenencias() == null) return;

    List<CriterioDePertenenciaOutputDTO> lista = coleccion.getCriteriosDePertenencias();

    for (int i = 0; i < lista.size(); i++) {
      CriterioDePertenenciaOutputDTO c = lista.get(i);

      // A. VALIDAR CATEGORÍA
      if (c.getTipoCriterio() == TipoCriterio.CATEGORIA) {
        Object catObj = c.getParametros().get("categoria");
        if (catObj == null || catObj.toString().trim().isEmpty()) {
          bindingResult.rejectValue(
              "criteriosDePertenencias[" + i + "].parametros['categoria']",
              "error.categoria",
              "Debés seleccionar una categoría."
          );
        }
      }

      // B. VALIDAR FECHAS
      if (c.getTipoCriterio() == TipoCriterio.FECHA) {
        String inicioStr = (String) c.getParametros().get("fechaInicio");
        String finStr = (String) c.getParametros().get("fechaFin");
        boolean fechasCompletas = true;

        // B1. Validar vacíos
        if (inicioStr == null || inicioStr.trim().isEmpty()) {
          bindingResult.rejectValue(
              "criteriosDePertenencias[" + i + "].parametros['fechaInicio']",
              "error.fechaInicio",
              "La fecha de inicio es obligatoria."
          );
          fechasCompletas = false;
        }
        if (finStr == null || finStr.trim().isEmpty()) {
          bindingResult.rejectValue(
              "criteriosDePertenencias[" + i + "].parametros['fechaFin']",
              "error.fechaFin",
              "La fecha de fin es obligatoria."
          );
          fechasCompletas = false;
        }

        // B2. Validar Lógica (Inicio > Fin)
        if (fechasCompletas) {
          try {
            LocalDate inicio = LocalDate.parse(inicioStr);
            LocalDate fin = LocalDate.parse(finStr);

            if (inicio.isAfter(fin)) {
              bindingResult.rejectValue(
                  "criteriosDePertenencias[" + i + "].parametros['fechaInicio']",
                  "error.fechaCruzada",
                  "La fecha de inicio no puede ser posterior al fin."
              );
              // Marcamos también fechaFin (sin mensaje extra) para que se ponga rojo
              bindingResult.rejectValue(
                  "criteriosDePertenencias[" + i + "].parametros['fechaFin']",
                  "error.fechaCruzada",
                  ""
              );
            }
          } catch (DateTimeParseException e) {
            bindingResult.rejectValue(
                "criteriosDePertenencias[" + i + "].parametros['fechaInicio']",
                "error.formato", "Formato de fecha inválido"
            );
          }
        }
      }
    }
  }

  // ========================================================================================
  // GESTIÓN DE COLECCIONES
  // ========================================================================================

  @GetMapping("/colecciones")
  public String gestionarColecciones(Model model) {
    try {
      List<ColeccionInputDTO> colecciones = coleccionesApiService.obtenerColecciones();
      model.addAttribute("colecciones", colecciones);
    } catch (Exception e) {
      // Error al listar: logueamos y mostramos lista vacía + mensaje
      log.error("Error al listar colecciones", e);
      model.addAttribute("colecciones", new ArrayList<>());
      model.addAttribute("error", "No se pudieron cargar las colecciones. Intente más tarde.");
    }
    return "admin-colecciones";
  }

  @GetMapping("/colecciones/{id}/detalle")
  public String verDetalleColeccion(@PathVariable Long id,
                                    Model model) {
    // NO AGREGO TRY-CATCH PARA Q EL ERROR LO ATRAPE EL CONTROLLER ADVICE
    ColeccionInputDTO coleccion = this.coleccionesApiService.obtenerColeccion(id);
    model.addAttribute("coleccion", coleccion);
    return "admin-coleccion-detalle";
  }

  @GetMapping("/colecciones/crear")
  public String mostrarFormularioCreacion(Model model) {
    model.addAttribute("coleccion", new ColeccionOutputDTO());

    this.cargarFuentesEnModelo(model);
    this.cargarCategoriasEnModelo(model);

    return "admin-crear-coleccion";
  }

  @PostMapping("/colecciones/crear")
  public String crearColeccion(@ModelAttribute("coleccion") @Valid ColeccionOutputDTO coleccionOutputDTO,
                               BindingResult bindingResult,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {

    // 1. Verificar Sesión
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    if (authData == null || authData.getAccessToken() == null) {
      return "redirect:/auth/login";
    }

    // 2. VALIDACIÓN MANUAL (Inyecta errores en BindingResult)
    this.validarCriterios(coleccionOutputDTO, bindingResult);

    // 3. Chequeo de errores (Locales + Manuales)
    if (bindingResult.hasErrors()) {
      this.cargarFuentesEnModelo(model);
      this.cargarCategoriasEnModelo(model);
      return "admin-crear-coleccion";
    }

    // 4. Llamada al Servicio
    try {
      coleccionesApiService.crearColeccion(coleccionOutputDTO, authData.getAccessToken()).block();

      redirectAttributes.addFlashAttribute("mensaje", "¡Colección creada exitosamente!");
      return "redirect:/admin/colecciones";

    } catch (ApiException ex) {
      // 5. Manejo de Errores de API (400, 409, 422)
      ApiError apiError = ex.getApiError();

      if (apiError != null) {
        // A. Inyectar errores de campos en BindingResult
        if (apiError.fields() != null) {
          apiError.fields().forEach((campo, msg) ->
              bindingResult.rejectValue(campo, "api.error", msg)
          );
        }
        // B. Mensaje global
        if (apiError.message() != null) {
          model.addAttribute("globalError", apiError.message());
        }
        // C. Detalles técnicos
        if (apiError.details() != null && !apiError.details().isEmpty()) {
          model.addAttribute("errorDetails", apiError.details());
        }
      } else {
        model.addAttribute("globalError", "Error al crear la colección: " + ex.getMessage());
      }

      // Recargamos el modelo y volvemos a la vista con los errores marcados
      model.addAttribute("coleccion", coleccionOutputDTO);
      this.cargarFuentesEnModelo(model);
      this.cargarCategoriasEnModelo(model);
      return "admin-crear-coleccion";

    } catch (Exception ex) {
      // 6. Catch-all
      log.error("💀 Error inesperado creando colección: ", ex);
      model.addAttribute("globalError", "Ocurrió un error inesperado. Por favor, intente nuevamente.");
      model.addAttribute("coleccion", coleccionOutputDTO);
      this.cargarFuentesEnModelo(model);
      this.cargarCategoriasEnModelo(model);
      return "admin-crear-coleccion";
    }
  }

  @GetMapping("/colecciones/{id}/editar")
  public String mostrarFormularioEdicion(@PathVariable Long id,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
    try {
      // 1. Obtener datos actuales
      ColeccionInputDTO existente = coleccionesApiService.obtenerColeccion(id);

      // 2. Mapear a DTO de Formulario (OutputDTO)
      ColeccionOutputDTO form = new ColeccionOutputDTO();
      form.setId(existente.getId());
      form.setTitulo(existente.getTitulo());
      form.setDescripcion(existente.getDescripcion());
      form.setAlgoritmoConsenso(existente.getAlgoritmoConsenso());

      // Mapeo de Fuentes (IDs)
      if (existente.getFuentes() != null) {
        List<Long> ids = existente.getFuentes().stream().map(FuenteInputDTO::getId).toList();
        form.setFuentesIds(ids);
      } else {
        form.setFuentesIds(new ArrayList<>());
      }

      // Mapeo de Criterios
      if (existente.getCriteriosDePertenencias() != null) {
        List<CriterioDePertenenciaOutputDTO> criteriosOutput = existente.getCriteriosDePertenencias().stream()
            .map(c -> {
              CriterioDePertenenciaOutputDTO out = new CriterioDePertenenciaOutputDTO();
              out.setId(c.getId());
              out.setNombreCriterio(c.getNombreCriterio());
              out.setTipoCriterio(c.getTipoCriterio());
              // Copia segura de parámetros
              out.setParametros(c.getParametros() != null ? new HashMap<>(c.getParametros()) : new HashMap<>());
              return out;
            }).toList();
        form.setCriteriosDePertenencias(criteriosOutput);
      } else {
        form.setCriteriosDePertenencias(new ArrayList<>());
      }

      // 3. Cargar auxiliares
      this.cargarCategoriasEnModelo(model);
      this.cargarFuentesEnModelo(model);

      model.addAttribute("coleccion", form);
      model.addAttribute("idColeccion", id);

      return "admin-editar-coleccion";

    } catch (Exception e) {
      log.error("Error al cargar formulario edición colección {}", id, e);
      redirectAttributes.addFlashAttribute("error", "No se pudo cargar la colección para editar.");
      return "redirect:/admin/colecciones";
    }
  }

  @PostMapping("/colecciones/{id}/editar")
  public String procesarEdicion(@PathVariable Long id,
                                @ModelAttribute("coleccion") @Valid ColeccionOutputDTO coleccionOutputDTO,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {

    // 1. Verificación de Sesión
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    if (authData == null || authData.getAccessToken() == null) {
      return "redirect:/auth/login";
    }

    // 2. VALIDACIÓN MANUAL (Reutilizamos la lógica)
    this.validarCriterios(coleccionOutputDTO, bindingResult);

    // 3. Chequeo de errores
    if (bindingResult.hasErrors()) {
      model.addAttribute("idColeccion", id);
      this.cargarFuentesEnModelo(model);
      this.cargarCategoriasEnModelo(model);
      return "admin-editar-coleccion";
    }

    // 4. Llamada al Servicio
    try {
      coleccionesApiService.modificarColeccion(id, coleccionOutputDTO, authData.getAccessToken());

      redirectAttributes.addFlashAttribute("mensaje", "Colección modificada con éxito.");
      return "redirect:/admin/colecciones/" + id + "/detalle";

    } catch (ApiException ex) {
      // 5. Manejo de Errores de API
      ApiError apiError = ex.getApiError();

      if (apiError != null) {
        // A. Inyectar errores de campos
        if (apiError.fields() != null) {
          apiError.fields().forEach((campo, msg) ->
              bindingResult.rejectValue(campo, "api.error", msg)
          );
        }
        // B. Mensaje global
        if (apiError.message() != null) {
          model.addAttribute("globalError", apiError.message());
        }
      } else {
        model.addAttribute("globalError", "Error al guardar los cambios: " + ex.getMessage());
      }

      model.addAttribute("idColeccion", id);
      model.addAttribute("coleccion", coleccionOutputDTO);
      this.cargarFuentesEnModelo(model);
      this.cargarCategoriasEnModelo(model);
      return "admin-editar-coleccion";

    } catch (Exception e) {
      // 6. Catch-all
      log.error("💀 Error inesperado editando colección {}: ", id, e);
      model.addAttribute("globalError", "Ocurrió un error inesperado. Intente nuevamente.");

      model.addAttribute("idColeccion", id);
      model.addAttribute("coleccion", coleccionOutputDTO);
      this.cargarFuentesEnModelo(model);
      this.cargarCategoriasEnModelo(model);
      return "admin-editar-coleccion";
    }
  }

  @GetMapping("/colecciones/{id}/eliminar")
  public String eliminarColeccion(@PathVariable Long id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {

    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    if (authData == null || authData.getAccessToken() == null) return "redirect:/auth/login";

    try {
      coleccionesApiService.eliminarColeccion(id, authData.getAccessToken());
      redirectAttributes.addFlashAttribute("mensaje", "Colección eliminada correctamente.");
    } catch (ApiException ex) {
      log.warn("Error API eliminando colección: {}", ex.getMessage());
      redirectAttributes.addFlashAttribute("error", "No se pudo eliminar: " + (ex.getApiError() != null ? ex.getApiError().message() : ex.getMessage()));
    } catch (Exception e) {
      log.error("Error fatal eliminando colección {}", id, e);
      redirectAttributes.addFlashAttribute("error", "Ocurrió un error inesperado al eliminar.");
    }
    return "redirect:/admin/colecciones";
  }

  // ========================================================================================
  // DASHBOARD & REVISIONES
  // ========================================================================================

  @GetMapping("/dashboard")
  public String mostrarDashboard(Model model, Authentication authentication) {
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    String token = authData.getAccessToken();

    DashboardSummaryDTO summary;

    try {
      summary = dashboardApiService.getSummary(token);
    } catch (Exception e) {
      log.error("Fallo al obtener resumen del dashboard: {}", e.getMessage());
      summary = DashboardSummaryDTO.builder()
          .hechosPendientes(0L).solicitudesEliminacion(0L)
          .solicitudesModificacion(0L).coleccionesActivas(0L).build();
      model.addAttribute("error", "Fallo la carga de estadísticas.");
    }
    model.addAttribute("summary", summary);

    try {
      List<ActividadDTO> actividad = dashboardApiService.obtenerActividadReciente(token);
      model.addAttribute("actividadReciente", actividad);
    } catch (Exception e) {
      model.addAttribute("actividadReciente", Collections.emptyList());
    }

    return "admin-dashboard";
  }

  @GetMapping("/revisiones")
  public String gestionarRevisiones(Model model, Authentication authentication) {
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    String token = authData.getAccessToken();

    try {
      var hechos = revisionesApiService.obtenerHechosPendientes(token);
      var solicitudesEliminacion = solicitudesEliminacionApiService.obtenerSolicitudesPendientes(token);
      var solicitudesModificacion = solicitudesModificacionApiService.obtenerSolicitudesModificacionPendientes();

      model.addAttribute("hechosPendientes", hechos);
      model.addAttribute("solicitudesPendientes", solicitudesEliminacion);
      model.addAttribute("modificacionesPendientes", solicitudesModificacion);
    } catch (Exception e) {
      log.error("Error cargando revisiones", e);
      model.addAttribute("error", "Error cargando listas de revisión.");
      model.addAttribute("hechosPendientes", new ArrayList<>());
      model.addAttribute("solicitudesPendientes", new ArrayList<>());
      model.addAttribute("modificacionesPendientes", new ArrayList<>());
    }

    return "admin-revisiones";
  }

  // ========================================================================================
  // DETALLE Y EDICIÓN DE HECHOS (REVISIONES)
  // ========================================================================================

  @GetMapping("/revisiones/hechos/{id}/detalle")
  public String verDetalleHecho(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
    try {
      HechoInputDTO hecho = this.hechosApiService.obtenerHecho(id);
      model.addAttribute("hecho", hecho);
      model.addAttribute("id", id);
      return "admin-hecho-detalle";
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "No se pudo cargar el hecho.");
      return "redirect:/admin/revisiones";
    }
  }

  @GetMapping("/revisiones/hechos/{id}/editar")
  public String editarHecho(@PathVariable Long id, Model model) {
    try {
      HechoInputDTO inputOriginal = this.hechosApiService.obtenerHecho(id);
      EditarHechoDTO hecho = this.hechoMapper.toEditarHechoDTO(inputOriginal);

      model.addAttribute("hecho", hecho);
      model.addAttribute("id", id);

      this.cargarCategoriasEnModelo(model);

      return "admin-hecho-editar";
    } catch (Exception e) {
      return "redirect:/admin/revisiones";
    }
  }

  @PostMapping("/revisiones/hechos/{id}/editar")
  public String editarHechoPost(@PathVariable Long id,
                                @ModelAttribute("hecho") @Valid EditarHechoDTO hecho,
                                BindingResult bindingResult,
                                @RequestParam(value = "nuevasImagenes", required = false) List<MultipartFile> multipartFiles,
                                Model model,
                                RedirectAttributes redirectAttributes) {

    Map<String, String> erroresVista = new HashMap<>();

    if (bindingResult.hasErrors()) {
      bindingResult.getFieldErrors().forEach(e -> erroresVista.put(e.getField(), e.getDefaultMessage()));
      model.addAttribute("hecho", hecho);
      model.addAttribute("id", id);
      model.addAttribute("errores", erroresVista);
      this.cargarCategoriasEnModelo(model);
      return "admin-hecho-editar";
    }

    // Procesar imágenes
    if (hecho.getMultimedia() == null) hecho.setMultimedia(new ArrayList<>());
    if (multipartFiles != null) {
      for (MultipartFile file : multipartFiles) {
        if (!file.isEmpty()) {
          try {
            String name = imagenesService.copy(file);
            hecho.getMultimedia().add(name);
          } catch (IOException e) {
            model.addAttribute("globalError", "Error al subir imagen.");
            model.addAttribute("hecho", hecho);
            model.addAttribute("id", id);
            this.cargarCategoriasEnModelo(model);
            return "admin-hecho-editar";
          }
        }
      }
    }

    try {
      this.hechosApiService.editarHecho(id, hecho);
      redirectAttributes.addFlashAttribute("mensaje", "¡Hecho editado con éxito!");
      return "redirect:/admin/revisiones/hechos/" + id + "/detalle";

    } catch (ApiException ex) {
      ApiError apiError = ex.getApiError();
      if (apiError != null) {
        if (apiError.fields() != null) erroresVista.putAll(apiError.fields());
        if (apiError.message() != null) model.addAttribute("globalError", apiError.message());
      }
      model.addAttribute("hecho", hecho);
      model.addAttribute("id", id);
      model.addAttribute("errores", erroresVista);
      this.cargarCategoriasEnModelo(model);
      return "admin-hecho-editar";
    } catch (Exception ex) {
      log.error("Error inesperado editando hecho", ex);
      model.addAttribute("globalError", "Ocurrió un error inesperado.");
      model.addAttribute("id", id);
      model.addAttribute("hecho", hecho);
      this.cargarCategoriasEnModelo(model);
      return "admin-hecho-editar";
    }
  }

  @PostMapping("/revisiones/hechos/{id}/{accion}")
  public String accionesHecho(@PathVariable Long id,
                              @PathVariable String accion,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    try {
      if ("aprobar".equals(accion)) {
        revisionesApiService.aprobarHecho(id, authData.getAccessToken());
        redirectAttributes.addFlashAttribute("mensaje", "¡Hecho aprobado con éxito!");
      } else if ("rechazar".equals(accion)) {
        revisionesApiService.rechazarHecho(id, authData.getAccessToken());
        redirectAttributes.addFlashAttribute("mensaje", "¡Hecho rechazado con éxito!");
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al procesar el hecho: " + e.getMessage());
    }
    return "redirect:/admin/revisiones";
  }

  // ========================================================================================
  // SOLICITUDES DE MODIFICACIÓN Y ELIMINACIÓN
  // ========================================================================================

  @GetMapping("/revisiones/modificaciones/{id}/detalle")
  public String verDetalleModificacion(@PathVariable Long id,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
    try {
      SolicitudModificacionInputDTO solicitudModificacion = this.solicitudesModificacionApiService.obtenerSolicitud(id);

      // Cargar el hecho original para la comparación
      HechoInputDTO hechoOriginal = this.hechosApiService.obtenerHecho(solicitudModificacion.getHechoId());

      model.addAttribute("solicitudModificacion", solicitudModificacion);
      model.addAttribute("hechoOriginal", hechoOriginal);

      return "admin-detalle-modificacion";

    } catch (Exception e) {
      log.error("Error al cargar detalle de solicitud modificacion {}", id, e);
      redirectAttributes.addFlashAttribute("error", "Error al cargar la solicitud.");
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
        redirectAttributes.addFlashAttribute("mensaje", "¡Solicitud de modificación aprobada!");
      } else if ("rechazar".equals(accion)) {
        solicitudesModificacionApiService.rechazarSolicitudModificacion(id);
        redirectAttributes.addFlashAttribute("mensaje", "¡Solicitud de modificación rechazada!");
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
    }
    return "redirect:/admin/revisiones";
  }

  @GetMapping("/revisiones/solicitudes/{id}/detalle")
  public String verDetalleEliminacion(@PathVariable Long id,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
    try {
      SolicitudEliminacionInputDTO solicitudEliminacion = this.solicitudesEliminacionApiService.obtenerSolicitud(id);
      // Cargar hecho original para mostrar qué se va a eliminar
      HechoInputDTO hechoOriginal = this.hechosApiService.obtenerHecho(solicitudEliminacion.getIdHecho());

      model.addAttribute("solicitudEliminacion", solicitudEliminacion);
      model.addAttribute("hechoOriginal", hechoOriginal);

      return "admin-detalle-eliminacion";

    } catch (Exception e) {
      log.error("Error al cargar detalle de solicitud eliminacion {}", id, e);
      redirectAttributes.addFlashAttribute("error", "Error al cargar la solicitud.");
      return "redirect:/admin/revisiones";
    }
  }

  @PostMapping("/revisiones/solicitudes/{id}/{accion}")
  public String accionesSolicitud(@PathVariable Long id,
                                  @PathVariable String accion,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    try {
      if ("aceptar".equals(accion)) {
        solicitudesEliminacionApiService.aceptarSolicitud(id, authData.getAccessToken());
        redirectAttributes.addFlashAttribute("mensaje", "¡Solicitud aceptada! Hecho eliminado.");
      } else if ("rechazar".equals(accion)) {
        solicitudesEliminacionApiService.rechazarSolicitud(id, authData.getAccessToken());
        redirectAttributes.addFlashAttribute("mensaje", "¡Solicitud rechazada!");
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
    }
    return "redirect:/admin/revisiones";
  }

  // ========================================================================================
  // FUENTES, IMPORTACIÓN Y ESTADÍSTICAS
  // ========================================================================================

  @GetMapping("/fuentes")
  public String mostrarFuentes(Model model) {
    this.cargarFuentesEnModelo(model);
    return "admin-fuentes";
  }

  @PostMapping("/importar-hechos")
  public String importarHechos(@RequestParam("archivoCsv") MultipartFile file,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
    try {
      dashboardApiService.importarHechos(file, authData.getAccessToken());
      redirectAttributes.addFlashAttribute("mensaje", "Archivo enviado a procesar.");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al subir archivo: " + e.getMessage());
    }
    return "redirect:/admin/dashboard";
  }

    @GetMapping("/estadisticas")
    public String mostrarEstadisticas(
            @RequestParam(value = "top", required = false) Boolean top,
            @RequestParam(value = "categorias", required = false) List<String> categorias,

            // Paginación para CATEGORÍAS
            @RequestParam(defaultValue = "0") int catPage,
            @RequestParam(defaultValue = "10") int catSize,

            // Paginación para COLECCIONES
            @RequestParam(defaultValue = "0") int colPage,
            @RequestParam(defaultValue = "10") int colSize,

            Model model) {
        try {
            // 1. Obtener la página de CATEGORÍAS paginada
            Page<CategoriaEstadisticaDTO> paginaCategorias =
                    estadisticasApiService.obtenerCategoriasPaginadas(categorias, top, catPage, catSize);

            Page<CategoriaEstadisticaDTO> categoriaMax =
                    estadisticasApiService.obtenerCategoriasPaginadas(categorias, true, 0, 1);
//            // 2. Obtener la Categoría Máxima (Sin paginación)
//            List<CategoriaEstadisticaDTO> categoriaMaxList = estadisticasApiService.obtenerCategorias(List.of(),true);
//            CategoriaEstadisticaDTO categoriaMax = !categoriaMaxList.isEmpty() ? categoriaMaxList.get(0) : null;

            // 3. Obtener la página de COLECCIONES paginada
            Page<ColeccionEstadisticaDTO> paginaColecciones =
                    estadisticasApiService.obtenerColeccionesPaginadas(List.of(), colPage, colSize); // Asumo List.of() o filtro si tienes uno

            // 4. Agregar al modelo
            // CATEGORÍAS
            model.addAttribute("paginaCategorias", paginaCategorias);
            model.addAttribute("categorias", paginaCategorias.getContent());

            // COLECCIONES
            model.addAttribute("paginaColecciones", paginaColecciones); // Nuevo objeto Page
            model.addAttribute("colecciones", paginaColecciones.getContent()); // Contenido para la tabla

            // OTROS
            model.addAttribute("categoriaMaxima", categoriaMax.getContent().get(0));
            // ... (otros atributos) ...

            // Filtros y Paginación (para los enlaces de navegación)
            model.addAttribute("filtroCategorias", categorias);
            model.addAttribute("filtroTop", top);

            // Parámetros de paginación de CATEGORÍAS
            model.addAttribute("catPage", catPage);
            model.addAttribute("catSize", catSize);

            // Parámetros de paginación de COLECCIONES
            model.addAttribute("colPage", colPage);
            model.addAttribute("colSize", colSize);

        } catch (Exception e) {
            // ...
        }
        return "admin-estadisticas";
    }
}