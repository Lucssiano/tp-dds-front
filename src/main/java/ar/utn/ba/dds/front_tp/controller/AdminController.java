package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionDTO;
import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.dto.admin.DashboardSummaryDTO;
import ar.utn.ba.dds.front_tp.services.ColeccionesApiService;
import ar.utn.ba.dds.front_tp.services.DashboardApiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

  @PostMapping("/colecciones/crear")
  public String crearColeccion(@ModelAttribute("coleccionNueva") ColeccionInputDTO coleccionInput,
                               Authentication authentication, // Inyectamos Authentication
                               RedirectAttributes redirectAttributes) {

    // Obtenemos el DTO de los "detalles" del objeto Authentication
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
}

