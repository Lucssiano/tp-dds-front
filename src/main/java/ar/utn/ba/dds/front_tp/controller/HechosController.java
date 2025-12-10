package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.Utils.JwtUtils;
import ar.utn.ba.dds.front_tp.dto.editar.EditarHechoDTO;
import ar.utn.ba.dds.front_tp.dto.input.CategoriaInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.ApiError;
import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.ColeccionOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.SolicitudEliminacionOutputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.exceptions.api.ApiException;
import ar.utn.ba.dds.front_tp.mappers.HechoMapper;
import ar.utn.ba.dds.front_tp.services.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.Principal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
@Controller
@RequestMapping("/hechos")
@RequiredArgsConstructor
public class HechosController {
  private static final Logger log = LoggerFactory.getLogger(HechosController.class);
  private final HechosApiService hechosApiService;
  private final SolicitudesEliminacionApiService solicitudesEliminacionApiService;
  private final SolicitudesModificacionApiService solicitudesModificacionApiService;
  @Autowired
  private  final UploadFileService imagenesService;
  private final ObjectMapper objectMapper;
  private final HechoMapper hechoMapper;

  private final ColeccionesApiService coleccionesApiService;
  private final FuentesApiService fuentesApiService;

  private void cargarFiltrosEnModelo(Model model) {
    // 1. Cargar Top 3 Colecciones
    List<ColeccionOutputDTO> todasCols = coleccionesApiService.obtenerColeccionesOutput();
    if (todasCols != null) {
      int limite = Math.min(todasCols.size(), 3);
      model.addAttribute("listaColecciones", todasCols.subList(0, limite));
    }

    // 2. Cargar Fuentes
    model.addAttribute("listaFuentes", fuentesApiService.obtenerFuentes());

    // 3. Cargar Categorías
    model.addAttribute("listaCategorias", hechosApiService.obtenerCategoriasOutput());
  }

  private void cargarCategoriasEnModelo(Model model) {
    try {
      List<CategoriaInputDTO> categorias = this.hechosApiService.obtenerCategorias();
      model.addAttribute("categorias", categorias);
    } catch (Exception ex) {
      if (ex instanceof ApiException) {
        log.warn("⚠️ No se cargaron las categorías. Causa: {}", ex.getMessage());
      } else {
        log.error("🔥 BUG: Falló la carga de categorías.", ex);
      }
      // Lista vacía para evitar errores en la vista
      model.addAttribute("categorias", new ArrayList<CategoriaInputDTO>());
      model.addAttribute("warningCategorias", "No se pudieron cargar las categorías existentes.");
    }
  }

  @GetMapping("/mapa")
  public String mostrarMapa(
      @RequestParam(required = false, name = "fechaAcontecimientoDesde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
      @RequestParam(required = false, name = "fechaAcontecimientoHasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
      @RequestParam(required = false) List<Long> categorias,
      @RequestParam(required = false) List<Long> fuentes,
      Model model) {
    try {
      cargarFiltrosEnModelo(model);

      // Llamada al servicio
      List<HechoInputDTO> hechos = this.hechosApiService.obtenerHechos(fechaDesde, fechaHasta, categorias, fuentes);
      log.info("Cantidad de hechos recibidos: {}", hechos.size());

      // Serialización
      String hechosJson = objectMapper.writeValueAsString(hechos);

      // Modelo
      model.addAttribute("hechosJson", hechosJson);
      model.addAttribute("fechaDesde", fechaDesde != null ? fechaDesde.toString() : "");
      model.addAttribute("fechaHasta", fechaHasta != null ? fechaHasta.toString() : "");
      model.addAttribute("categoriasSeleccionadas", categorias != null ? categorias : new ArrayList<>());
      model.addAttribute("fuentesSeleccionadas", fuentes != null ? fuentes : new ArrayList<>());
      model.addAttribute("idColeccionActual", null);

    } catch (Exception e) {
      log.error("Error al obtener hechos o al convertirlos a JSON", e);
      // Manejo elegante del error en la vista
      model.addAttribute("hechosJson", "[]");
      model.addAttribute("categoriasSeleccionadas", new ArrayList<>());
      model.addAttribute("fuentesSeleccionadas", new ArrayList<>());
    }
    return "mapa";
  }

  @GetMapping("/mapa/coleccion/{id}")
  public String verHechosColeccion(
      @PathVariable("id") Long idColeccion,
      @RequestParam(required = false, defaultValue = "CURADA") String modo,
      @RequestParam(required = false, name = "fechaAcontecimientoDesde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
      @RequestParam(required = false, name = "fechaAcontecimientoHasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
      @RequestParam(required = false) List<Long> categorias,
      @RequestParam(required = false) List<Long> fuentes,
      Model model) {
    try {
      cargarFiltrosEnModelo(model);

      // Llamada al servicio
      List<HechoInputDTO> hechos = this.coleccionesApiService.obtenerHechosColeccion(
          idColeccion, modo, fechaDesde, fechaHasta, categorias, fuentes
      );

      // Serialización
      String hechosJson = objectMapper.writeValueAsString(hechos);

      // Modelo
      model.addAttribute("hechosJson", hechosJson);
      model.addAttribute("modoActual", modo);
      model.addAttribute("fechaDesde", fechaDesde != null ? fechaDesde.toString() : "");
      model.addAttribute("fechaHasta", fechaHasta != null ? fechaHasta.toString() : "");
      model.addAttribute("categoriasSeleccionadas", categorias != null ? categorias : new ArrayList<>());
      model.addAttribute("fuentesSeleccionadas", fuentes != null ? fuentes : new ArrayList<>());
      model.addAttribute("idColeccionActual", idColeccion);

      return "mapa";
    } catch (Exception e) {
      log.error("Error cargando mapa de colección: {}", e.getMessage(), e);
      model.addAttribute("errorGlobal", "Ocurrió un error inesperado al cargar la colección.");
      return "home";
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
      try {
        log.info("Hechos que me traje edl usuario cantidad: " + hechosUsuario.size());
      } catch (Exception e) {
        log.info("Bardie por lista nula " + hechosUsuario.size());
        throw new RuntimeException(e);
      }
      model.addAttribute("hechos", hechosUsuario);
      model.addAttribute("usuarioEmail", email);

      return "mis-hechos";
    } catch (Exception e) {
      log.error("Error al obtener hechos del usuario", e);
      model.addAttribute("errorGlobal", "Ocurrió un error al obtener tus hechos. Intenta más tarde.");
      return "mis-hechos";
    }
  }

  @GetMapping("/{id}/detalle")
  public String verDetalleHecho(@PathVariable Long id,
                                Model model,
                                Authentication authentication) {
    // NO AGREGO TRY-CATCH PARA Q EL ERROR LO ATRAPE EL CONTROLLER ADVICE
    var hecho = hechosApiService.obtenerHecho(id);
    model.addAttribute("hecho", hecho);
    List<String> rutasMultimedia = hecho.getMultimedia();
    // Nombrar la variable en el modelo como 'imagenes' para que coincida con la vista
    model.addAttribute("imagenes", rutasMultimedia);

    boolean esPropietario = false;

    if (authentication != null && authentication.getDetails() instanceof AuthResponseDTO token) {
      String email = JwtUtils.validarToken(token.getAccessToken());
      if (email != null && hecho.getUsuario() != null) {
        esPropietario = email.equalsIgnoreCase(hecho.getUsuario());
      }
    }

    model.addAttribute("esPropietario", esPropietario);

    return "hecho-detalle";
  }

  @GetMapping("/subir-hecho")
  public String subirHecho(Model model) {
    model.addAttribute("hecho", EditarHechoDTO.builder().build());
    this.cargarCategoriasEnModelo(model);
    return "subir-hecho";
  }

  @PostMapping("/crear-hecho")
  public String crearHecho(@ModelAttribute("hecho") @Valid EditarHechoDTO hecho,
                           BindingResult bindingResult,
                           @RequestParam(value = "nuevasImagenes", required = false) List<MultipartFile> multipartFiles,
                           Model model,
                           RedirectAttributes redirectAttributes,
                           Principal principal) {

    // -------------------------------------------------------------------
    // 0. EXTRACCIÓN DE DATOS DE SESIÓN (Usuario y Token)
    // -------------------------------------------------------------------
    String token = null;
    String usuarioEmail = "VISUALIZADOR/ANÓNIMO";

    // Lógica para obtener el token del SecurityContext
    if (principal != null) {
      usuarioEmail = principal.getName();
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
        try {
          AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
          token = authData.getAccessToken();
        } catch (Exception e) {
          // Logueamos pero no rompemos el flujo, seguimos intentando
          System.err.println("Advertencia: No se pudo extraer token de: " + principal.getName());
        }
      }
    }
    hecho.setUsuario(usuarioEmail);

    Map<String, String> erroresVista = new HashMap<>();

    // -------------------------------------------------------------------
    // A. VALIDACIÓN LOCAL (@NotNull, @Size, @NotEmpty)
    // -------------------------------------------------------------------
    if (bindingResult.hasErrors()) {
      bindingResult.getFieldErrors().forEach(e -> erroresVista.put(e.getField(), e.getDefaultMessage()));

      model.addAttribute("hecho", hecho);
      model.addAttribute("errores", erroresVista);

      this.cargarCategoriasEnModelo(model);
      return "subir-hecho";
    }

    // -------------------------------------------------------------------
    // B. PROCESAR FOTOS NUEVAS (Subida local temporal)
    // -------------------------------------------------------------------
    if (hecho.getMultimedia() == null) hecho.setMultimedia(new ArrayList<>());

    if (multipartFiles != null) {
      for (MultipartFile file : multipartFiles) {
        if (!file.isEmpty()) {
          try {
            String name = imagenesService.copy(file);
            hecho.getMultimedia().add(name);
          } catch (IOException e) {
            log.error("Error I/O al guardar imagen", e);
            model.addAttribute("globalError", "Error al subir imagen: " + file.getOriginalFilename());

            model.addAttribute("hecho", hecho);

            this.cargarCategoriasEnModelo(model);

            return "subir-hecho";
          }
        }
      }
    }

    // -------------------------------------------------------------------
    // C. LLAMADA AL SERVICIO
    // -------------------------------------------------------------------
    try {
      this.hechosApiService.crearHecho(hecho, token);

      redirectAttributes.addFlashAttribute("mensaje", "¡Hecho creado con éxito! Se ha enviado a moderación.");
      return "redirect:/hechos/subir-hecho";

    } catch (ApiException ex) {
      // ---------------------------------------------------------------
      // CASO UNIFICADO: Errores Controlados (400, 422, 409, 503)
      // ---------------------------------------------------------------
      // Atrapa ValidationException (campos) y GlobalBusinessException (servidor caído/reglas)

      ApiError apiError = ex.getApiError();

      if (apiError != null) {
        // 1. Si hay errores de campos específicos (422)
        if (apiError.fields() != null && !apiError.fields().isEmpty()) {
          erroresVista.putAll(apiError.fields());
        }

        // 2. Si hay mensaje global (503, 409 o 422 con mensaje)
        if (apiError.message() != null) {
          model.addAttribute("globalError", apiError.message());
        }

        // 3. Detalles técnicos
        if (apiError.details() != null && !apiError.details().isEmpty()) {
          model.addAttribute("errorDetails", apiError.details());
        }
      }

      // Restauramos estado
      model.addAttribute("hecho", hecho);
      model.addAttribute("errores", erroresVista);

      this.cargarCategoriasEnModelo(model);
      return "subir-hecho";

    } catch (Exception ex) {
      // ---------------------------------------------------------------
      // CASO CATCH-ALL: Bugs inesperados (Red de seguridad)
      // ---------------------------------------------------------------
      log.error("💀 Error inesperado no controlado al crear hecho: ", ex);

      model.addAttribute("globalError", "Ocurrió un error inesperado en la aplicación. Por favor, intente nuevamente.");
      model.addAttribute("hecho", hecho);

      this.cargarCategoriasEnModelo(model);
      return "subir-hecho";
    }
  }

  @GetMapping("/{id}/editar")
  public String editarHecho(@PathVariable Long id,
                            Model model) {
    // Usamos HechoInputDTO (con estructura anidada ubicacionInputDTO)
    HechoInputDTO inputOriginal = this.hechosApiService.obtenerHecho(id);

    // Mapeamos a EditarHechoDTO (plano)
    EditarHechoDTO hecho = this.hechoMapper.toEditarHechoDTO(inputOriginal);

    model.addAttribute("hecho", hecho);
    model.addAttribute("id", id);

    this.cargarCategoriasEnModelo(model);

    return "editar-hecho";
  }

  @PostMapping("/{id}/editar")
  public String subirHechoEditado(@PathVariable Long id,
                                  @ModelAttribute("hecho") @Valid EditarHechoDTO hecho,
                                  BindingResult bindingResult,
                                  @RequestParam(value = "nuevasImagenes", required = false) List<MultipartFile> multipartFiles,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
    Map<String, String> erroresVista = new HashMap<>();

    // A. VALIDACIÓN LOCAL
    if (bindingResult.hasErrors()) {
      bindingResult.getFieldErrors().forEach(e -> erroresVista.put(e.getField(), e.getDefaultMessage()));

      model.addAttribute("hecho", hecho);
      model.addAttribute("id", id);
      model.addAttribute("errores", erroresVista);

      this.cargarCategoriasEnModelo(model);

      return "editar-hecho";
    }

    // B. PROCESAR FOTOS NUEVAS
    if (hecho.getMultimedia() == null) hecho.setMultimedia(new ArrayList<>());

    if (multipartFiles != null) {
      for (MultipartFile file : multipartFiles) {
        if (!file.isEmpty()) {
          try {
            String name = imagenesService.copy(file);
            hecho.getMultimedia().add(name);
          } catch (IOException e) {
            log.error("Error I/O guardando imagen en edición", e);
            model.addAttribute("globalError", "Error al subir imagen: " + file.getOriginalFilename());

            model.addAttribute("hecho", hecho);
            model.addAttribute("id", id);

            this.cargarCategoriasEnModelo(model);

            return "editar-hecho";
          }
        }
      }
    }

    // C. LLAMADA AL SERVICIO
    try {
      this.solicitudesModificacionApiService.crearSolicitudModificacion(id, hecho);

      redirectAttributes.addFlashAttribute("mensaje", "¡Solicitud de edición creada con éxito! Se ha enviado a moderación.");
      return "redirect:/hechos/" + id + "/detalle";

    } catch (ApiException ex) {
      // D. ERROR DE NEGOCIO (ApiError)
      ApiError apiError = ex.getApiError();

      if (apiError != null) {
        // 1. Errores de campos (422)
        if (apiError.fields() != null && !apiError.fields().isEmpty()) {
          erroresVista.putAll(apiError.fields());
        }

        // 2. Mensaje global (409, 503, etc)
        if (apiError.message() != null) {
          model.addAttribute("globalError", apiError.message());
        }

        // 3. Detalles técnicos
        if (apiError.details() != null && !apiError.details().isEmpty()) {
          model.addAttribute("errorDetails", apiError.details());
        }
      }

      model.addAttribute("hecho", hecho);
      model.addAttribute("id", id);
      model.addAttribute("errores", erroresVista);

      this.cargarCategoriasEnModelo(model);

      return "editar-hecho";
    } catch (Exception ex) {
      // E. Catch-all (bugs inesperados)
      log.error("💀 Error inesperado editando hecho {}: ", id, ex);

      model.addAttribute("globalError", "Ocurrió un error inesperado al procesar la edición. Intente nuevamente.");

      model.addAttribute("hecho", hecho);
      model.addAttribute("id", id);

      this.cargarCategoriasEnModelo(model);

      return "editar-hecho";
    }
  }

  @GetMapping("/{id}/solicitud-eliminacion")
  public String mostrarFormularioSolicitudEliminacion(@PathVariable Long id,
                                                      Model model,
                                                      Authentication authentication) {
    // 1. Obtenemos el hecho para mostrar título y descripción
    HechoInputDTO hecho = this.hechosApiService.obtenerHecho(id);

    // 2. Preparamos el DTO vacío
    SolicitudEliminacionOutputDTO solicitud = SolicitudEliminacionOutputDTO.builder().build();
    solicitud.setIdHecho(hecho.getId());

    // 3. Prellenamos datos de usuario si corresponde (opcional, el post lo pisa igual por seguridad)
    if (authentication != null && authentication.isAuthenticated()) {
      AuthResponseDTO token = (AuthResponseDTO) authentication.getDetails();
      var email = JwtUtils.validarToken(token.getAccessToken());
      solicitud.setUsuario(email);
    } else {
      solicitud.setUsuario("VISUALIZADOR");
    }

    model.addAttribute("hecho", hecho);
    model.addAttribute("solicitud", solicitud);

    return "solicitud-eliminacion";
  }

  @PostMapping("/{id}/solicitud-eliminacion")
  public String enviarSolicitudEliminacion(@PathVariable Long id,
                                           @ModelAttribute("solicitud") @Valid SolicitudEliminacionOutputDTO solicitud,
                                           BindingResult bindingResult,
                                           Model model,
                                           RedirectAttributes redirectAttributes
  ) {
    Map<String, String> erroresVista = new HashMap<>();

    // A. VALIDACIÓN LOCAL (BindingResult)
    if (bindingResult.hasErrors()) {
      bindingResult.getFieldErrors().forEach(e -> erroresVista.put(e.getField(), e.getDefaultMessage()));

      HechoInputDTO hecho = this.hechosApiService.obtenerHecho(id);

      model.addAttribute("hecho", hecho);
      model.addAttribute("solicitud", solicitud);
      model.addAttribute("errores", erroresVista);

      return "solicitud-eliminacion";
    }

    // B. LLAMADA AL SERVICIO
    try {
      this.solicitudesEliminacionApiService.crearSolicitud(solicitud);

      redirectAttributes.addFlashAttribute("mensaje", "¡Solicitud de eliminación creada con éxito! Se ha enviado a moderación.");

      return "redirect:/hechos/" + id + "/detalle";

    } catch (ApiException ex) {
      // D. ERROR DE NEGOCIO (ApiError)
      ApiError apiError = ex.getApiError();

      if (apiError != null) {
        // 1. Errores de campos (422)
        if (apiError.fields() != null && !apiError.fields().isEmpty()) {
          erroresVista.putAll(apiError.fields());
        }

        // 2. Mensaje global (409, 503, etc)
        if (apiError.message() != null) {
          model.addAttribute("globalError", apiError.message());
        }

        // 3. Detalles técnicos
        if (apiError.details() != null && !apiError.details().isEmpty()) {
          model.addAttribute("errorDetails", apiError.details());
        }
      }

      // Recargamos vista con errores
      HechoInputDTO hecho = this.hechosApiService.obtenerHecho(id);

      model.addAttribute("hecho", hecho);
      model.addAttribute("solicitud", solicitud);
      model.addAttribute("errores", erroresVista);

      return "solicitud-eliminacion";

    } catch (Exception ex) {
      // E. Catch-all (bugs inesperados)
      log.error("💀 Error inesperado editando hecho {}: ", id, ex);

      model.addAttribute("globalError", "Ocurrió un error inesperado al procesar la edición. Intente nuevamente.");


      HechoInputDTO hecho = this.hechosApiService.obtenerHecho(id);

      model.addAttribute("hecho", hecho);
      model.addAttribute("solicitud", solicitud);
      model.addAttribute("errorGlobal", "Ocurrió un error inesperado al procesar la solicitud. Intente nuevamente.");

      return "solicitud-eliminacion";
    }
  }

  @GetMapping(value = "/uploads/{filename}")
  public ResponseEntity<Resource> goImage(@PathVariable String filename) {
      Resource resource = null;
      try {
          resource = imagenesService.load(filename);
      } catch (MalformedURLException e) {
          e.printStackTrace();
      }
      return ResponseEntity.ok()
              .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
              .body(resource);
  }
}
