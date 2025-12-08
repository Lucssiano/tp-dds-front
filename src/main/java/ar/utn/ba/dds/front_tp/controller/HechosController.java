package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.Utils.JwtUtils;
import ar.utn.ba.dds.front_tp.dto.editar.EditarHechoDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.CategoriaDTO;
import ar.utn.ba.dds.front_tp.dto.input.ApiError;
import ar.utn.ba.dds.front_tp.dto.input.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.input.SolicitudEliminacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.ColeccionOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.SoliOutputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.exceptions.api.ApiException;
import ar.utn.ba.dds.front_tp.exceptions.api.GlobalBusinessException;
import ar.utn.ba.dds.front_tp.exceptions.api.ValidationBusinessException;
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
  private final SolicitudesApiService solicitudesApiService;
  private final SolicitudesModificacionApiService solicitudesModificacionApiService;
  @Autowired
  private  final UploadFileService imagenesService;
  private final ObjectMapper objectMapper;
  private final HechoMapper hechoMapper;

  private final ColeccionesApiService coleccionesApiService; // <--- AGREGAR
  private final FuentesApiService fuentesApiService;         // <--- AGREGAR

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
      List<CategoriaDTO> categorias = this.hechosApiService.obtenerCategorias();
      model.addAttribute("categorias", categorias);
    } catch (Exception ex) {
      // Usamos 'Exception' para que sea una red de seguridad TOTAL.

      if (ex instanceof ApiException) {
        // Usamos WARN y solo mostramos el mensaje corto.
        log.warn("⚠️ No se cargaron las categorías. Causa: {}", ex.getMessage());
      } else {
        // Usamos ERROR y pasamos 'ex' como segundo argumento para ver el Stack Trace completo.
        log.error("🔥 BUG: Falló la carga de categorías por un error de código.", ex);
      }

      // Ponemos la lista vacía (CRÍTICO para que no rompa el HTML)
      model.addAttribute("categorias", new ArrayList<CategoriaDTO>());

      // Aviso visual amarillo
      model.addAttribute("warningCategorias", "No se pudieron cargar las sugerencias, pero podés escribir manualmente.");
    }
  }

  @GetMapping("/mapa")
  public String mostrarMapa(
      @RequestParam(required = false, defaultValue = "CURADA") String modo,
      @RequestParam(required = false, name = "fechaAcontecimientoDesde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
      @RequestParam(required = false, name = "fechaAcontecimientoHasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
      @RequestParam(required = false) List<Long> categorias, // <--- NUEVO
      @RequestParam(required = false) List<Long> fuentes,    // <--- NUEVO
      Model model) {
    try {
      // Cargamos los datos para los dropdowns y sidebar
      cargarFiltrosEnModelo(model);

      List<HechoInputDTO> hechos = hechosApiService.obtenerHechos(modo, fechaDesde, fechaHasta, categorias, fuentes);
      log.info("Cantidad de hechos recibidos: {}", hechos.size());

      // Convertimos la lista a un String JSON
      String hechosJson = objectMapper.writeValueAsString(hechos);

      // Pasamos el STRING JSON al modelo
      model.addAttribute("hechosJson", hechosJson);
      model.addAttribute("modoActual", modo);
      model.addAttribute("fechaDesde", fechaDesde != null ? fechaDesde.toString() : "");
      model.addAttribute("fechaHasta", fechaHasta != null ? fechaHasta.toString() : "");
      model.addAttribute("categoriasSeleccionadas", categorias);
      model.addAttribute("fuentesSeleccionadas", fuentes);
    } catch (Exception e) {
      log.error("Error al obtener hechos o al convertirlos a JSON", e);
      model.addAttribute("hechosJson", "[]"); // Pasamos un array vacío en caso de error
      model.addAttribute("modoActual", modo); // Pasamos un array vacío en caso de error
    }
    return "mapa";
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

      return "mis-hechos";  // => templates/mis-hechos.html
    } catch (Exception e) {
      log.error("Error al obtener hechos del usuario", e);
      model.addAttribute("errorGlobal", "Ocurrió un error al obtener tus hechos. Intenta más tarde.");
      return "mis-hechos"; // Mostramos la vista igual pero vacía
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
                            Model model,
                            RedirectAttributes redirectAttributes) {
    try {
      // Usamos HechoInputDTO (con estructura anidada ubicacionInputDTO)
      HechoInputDTO inputOriginal = this.hechosApiService.obtenerHecho(id);

      // Mapeamos a EditarHechoDTO (plano)
      EditarHechoDTO hecho = this.hechoMapper.toEditarHechoDTO(inputOriginal);

      model.addAttribute("hecho", hecho);
      model.addAttribute("id", id);

      return "editar-hecho";

    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "No se pudo cargar el hecho.");
      return "redirect:/hechos/mis-hechos";
    }
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

      model.addAttribute("hecho", hecho); // 'hecho' ya tiene la lista multimedia gracias a los hidden inputs
      model.addAttribute("id", id);
      model.addAttribute("errores", erroresVista);

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
            model.addAttribute("errorGlobal", "Error al subir imagen: " + file.getOriginalFilename());
            model.addAttribute("hecho", hecho);
            model.addAttribute("id", id);
            return "editar-hecho";
          }
        }
      }
    }

    // C. LLAMADA AL SERVICIO
    try {
      this.solicitudesModificacionApiService.crearSolicitudModificacion(id, hecho);

      redirectAttributes.addFlashAttribute("mensaje", "¡Solicitud de edición creada con éxito!");
      return "redirect:/hechos/" + id + "/detalle";

    } catch (ValidationBusinessException ex) {
      // D. ERROR DE NEGOCIO (ApiError)
      ApiError apiError = ex.getApiError();
      if (apiError.fields() != null) erroresVista.putAll(apiError.fields());
      if (apiError.message() != null) model.addAttribute("globalError", apiError.message());
      if (apiError.details() != null) model.addAttribute("errorDetails", apiError.details());

      model.addAttribute("hecho", hecho);
      model.addAttribute("id", id);
      model.addAttribute("errores", erroresVista);

      return "editar-hecho";
    } catch (GlobalBusinessException ex) {
      // E. Errores de Negocio/Sistema (409, 503, Connection Refused)
      // El usuario hizo todo bien, pero el sistema lo rechaza
      ApiError apiError = ex.getApiError();

      if (apiError.message() != null) model.addAttribute("globalError", apiError.message());
      if (apiError.details() != null) model.addAttribute("errorDetails", apiError.details());

      model.addAttribute("hecho", hecho);
      model.addAttribute("id", id);

      return "editar-hecho";
    }
  }

  @GetMapping("/mapa/coleccion/{id}")
  public String verHechosColeccion(@ModelAttribute("coleccion") ColeccionInputDTO coleccion,
                                   @RequestParam(required = false, defaultValue = "CURADA") String modoNavegacion,
                                   @RequestParam(required = false, name = "fechaAcontecimientoDesde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                                   @RequestParam(required = false, name = "fechaAcontecimientoHasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                                   @RequestParam(required = false) List<Long> categorias, // <--- NUEVO
                                   @RequestParam(required = false) List<Long> fuentes,    // <--- NUEVO
                                   Model model) {
    try {
      cargarFiltrosEnModelo(model);

      // Usamos el ID de la colección (asumo que 'coleccion' tiene el ID populado, sino usa @PathVariable)
      List<HechoInputDTO> hechos = hechosApiService.obtenerHechosColeccion(coleccion.getId(), modoNavegacion, fechaDesde, fechaHasta, categorias, fuentes);

      String hechosJson = objectMapper.writeValueAsString(hechos);

      model.addAttribute("hechosJson", hechosJson);
      model.addAttribute("modoActual", modoNavegacion);
      model.addAttribute("fechaDesde", fechaDesde != null ? fechaDesde.toString() : "");
      model.addAttribute("fechaHasta", fechaHasta != null ? fechaHasta.toString() : "");
      model.addAttribute("categoriasSeleccionadas", categorias);
      model.addAttribute("fuentesSeleccionadas", fuentes);

      return "mapa";
    } catch (Exception e) {
      log.error(e.getMessage(), e);
        model.addAttribute("errorGlobal", "Ocurrió un error inesperado: " + e.getMessage());
        return "home";
      }
    }

  @GetMapping("/{id}/solicitud-eliminacion")
  public String mostrarFormularioSolicitudEliminacion(@PathVariable Long id,
                                                      Model model,
                                                      Authentication authentication) {
    try {
      var hecho = hechosApiService.obtenerHecho(id);

      SolicitudEliminacionInputDTO solicitud = new SolicitudEliminacionInputDTO();
      solicitud.setIdHecho(hecho.getId());
      if (authentication != null) {
        AuthResponseDTO token = (AuthResponseDTO) authentication.getDetails();
        var email = JwtUtils.validarToken(token.getAccessToken());
        solicitud.setUsuario(email);
      } else {
        solicitud.setUsuario("VISUALIZADOR");
      }

      log.info("USUARIO" + solicitud.getUsuario());

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
      if (authentication != null) {
        AuthResponseDTO token = (AuthResponseDTO) authentication.getDetails();
        var email = JwtUtils.validarToken(token.getAccessToken());
        solicitud.setUsuario(email);
      } else {
        solicitud.setUsuario("VISUALIZADOR");
      }
      solicitud.setIdHecho(id);

      log.info("USUARIO justito antes de mandar" + solicitud.getUsuario());

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
