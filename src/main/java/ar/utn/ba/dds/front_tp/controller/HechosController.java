package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.Utils.JwtUtils;
import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.CategoriaDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.CrearHechoDTO;
import ar.utn.ba.dds.front_tp.dto.input.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.input.HechoInputDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.input.SolicitudEliminacionInputDTO;
import ar.utn.ba.dds.front_tp.dto.output.HechoOutputDTO;
import ar.utn.ba.dds.front_tp.dto.output.SoliOutputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.services.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.Principal;
import java.time.LocalDate;
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
import java.util.List;

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
  @Autowired
  private HttpSession session;
  private SecurityContextHolder securityContextHolder;

  @GetMapping("/mapa")
  public String mostrarMapa(
      @RequestParam(required = false, defaultValue = "CURADO") String modo,
      @RequestParam(required = false, name = "fechaAcontecimientoDesde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
      @RequestParam(required = false, name = "fechaAcontecimientoHasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
      Model model) {
    try {
      List<HechoInputDTO> hechos = hechosApiService.obtenerHechos(modo, fechaDesde, fechaHasta);
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
  public String subirHecho(Model model) {
    model.addAttribute("hecho", HechoOutputDTO.builder().build());
      try {
          // 1. Obtener las categorías del servicio
          List<CategoriaDTO> categorias = hechosApiService.obtenerCategorias();

          // 2. Agregar la lista de categorías al Model
          // Este atributo se usa en el th:each de la vista
          log.info("Cantidadcategorias"+ categorias.size());
          model.addAttribute("categorias", categorias);

      } catch (Exception e) {
          log.error("Error al obtener categorías para subir-hecho", e);
          // En caso de error, pasamos una lista vacía para evitar errores en la vista.
          model.addAttribute("categorias", new ArrayList<CategoriaDTO>());
          model.addAttribute("errorGlobal", "Error al cargar las categorías. Intente más tarde.");
      }
    return "subir-hecho";
  }

  @GetMapping("/mapa/coleccion/{id}")
  public String verHechosColeccion(@ModelAttribute("coleccion") ColeccionInputDTO coleccion,
                                   @RequestParam(required = false, defaultValue = "CURADO") String modo,
                                   @RequestParam(required = false, name = "fechaAcontecimientoDesde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                                   @RequestParam(required = false, name = "fechaAcontecimientoHasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                                   Model model) {
    try {
      List<HechoInputDTO> hechos = hechosApiService.obtenerHechosColeccion(coleccion.getId());

      String hechosJson = objectMapper.writeValueAsString(hechos);

      model.addAttribute("hechosJson", hechosJson);
      model.addAttribute("modoActual", modo);
      model.addAttribute("fechaDesde", fechaDesde != null ? fechaDesde.toString() : "");
      model.addAttribute("fechaHasta", fechaHasta != null ? fechaHasta.toString() : "");

      return "mapa";
    } catch (Exception e) {
      log.error(e.getMessage(), e);
        model.addAttribute("errorGlobal", "Ocurrió un error inesperado: " + e.getMessage());
        return "home";
      }
    }

    @PostMapping("/crear-hecho")
    public String crearHecho(@ModelAttribute("hecho") HechoOutputDTO hecho, //habia un @Valid que quite, tal vez tenga que volver a ponerlo y la dependencia
                             BindingResult bindingResult,
                             @RequestParam("multimediaFiles") List<MultipartFile> multipartFiles,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Principal principal) { // Usamos Principal para obtener el nombre de usuario de forma segura

        String token = null;
        String usuarioEmail = "VISUALIZADOR/ANÓNIMO";

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. LÓGICA DE EXTRACCIÓN DE USUARIO SEGURO (Mantenemos el código original)
        if (principal != null) {
            usuarioEmail = principal.getName();
            if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
                try {
                    AuthResponseDTO authData = (AuthResponseDTO) authentication.getDetails();
                    token = authData.getAccessToken();
                } catch (Exception e) {
                    System.err.println("Advertencia: Fallo al castear token para usuario: " + principal.getName());
                    token = null;
                }
            }
        }
        // -------------------------------------------------------------------

        // 2. VALIDACIÓN DEL LADO DEL SERVIDOR (SI FALLA, SE QUEDA en la vista POST)
        if (bindingResult.hasErrors()) {
            log.warn("Errores de validación encontrados.");
            // Devuelve el formulario con los errores de Thymeleaf
            return "subir-hecho";
        }
        // ----------------------------------------------------------------------

        // 3. PROCESAMIENTO DE ARCHIVOS (Mantenemos el código original)
        if (multipartFiles != null && !multipartFiles.isEmpty()) {
            List<String> nombresGuardados = new ArrayList<>();
            multipartFiles.forEach(f -> System.out.println(" - " + f.getOriginalFilename()));
            for (MultipartFile file : multipartFiles) {
                if (!file.isEmpty()) {
                    try {
                        String uniqueFileName = imagenesService.copy(file);
                        nombresGuardados.add(uniqueFileName);
                    } catch (IOException e) {
                        // Si falla el guardado de archivos, manejamos como un error del servicio.
                        redirectAttributes.addFlashAttribute("errorGlobal", "Error al guardar archivos multimedia: " + e.getMessage());
                        return "redirect:/hechos/subir-hecho";
                    }
                }
            }
            hecho.setMultimedia(nombresGuardados);
        }

        hecho.setUsuario(usuarioEmail);


        try {
            CrearHechoDTO payload = new CrearHechoDTO();
            payload.setHecho(hecho);
            payload.setAccessToken(token);

            // LLAMADA AL SERVICIO
            hechosApiService.crearHecho(payload, token);

            // 🚨 CAMINO DE ÉXITO (PRG) 🚨
            // Usamos Flash Attributes para llevar el mensaje a la siguiente petición GET
            redirectAttributes.addFlashAttribute("mensaje", "¡Hecho creado con éxito! Se ha enviado a moderación.");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");

            // Redirecciona al GET de la página del formulario
            return "redirect:/hechos/subir-hecho";

        } catch (Exception e) {
            log.error("Error al crear hecho (Redireccionando con error)", e);

            // 🚨 CAMINO DE ERROR DEL SERVICIO (PRG) 🚨
            // Usamos Flash Attributes para llevar el mensaje de error del servicio
            redirectAttributes.addFlashAttribute("errorGlobal", "Error al guardar el hecho: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");

            // Redirecciona al GET para mostrar el error sin el problema de recarga
            return "redirect:/hechos/subir-hecho";
        }
    }

  @GetMapping("/{id}/detalle")
  public String verDetalleHecho(@PathVariable Long id,
                                Model model,
                                Authentication authentication) {
    try {
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

  @GetMapping("/{id}/editar")
  public String editarHecho(@PathVariable Long id, Model model, Authentication authentication) {

    HechoInputDTO hecho = hechosApiService.obtenerHecho(id);

    model.addAttribute("hecho", hecho);
    return "editar-hecho";
  }

  @PostMapping("/{id}/editar")
  public String subirHechoEditado(@PathVariable Long id, @ModelAttribute("hecho") HechoInputDTO hechoInputDTO, Authentication authentication) {
    try{this.solicitudesModificacionApiService.crearSolicitudModificacion(id, hechoInputDTO);
    } catch (Exception e){
      log.error(e.getMessage());
    }
    return "redirect:/hechos/mis-hechos";
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
