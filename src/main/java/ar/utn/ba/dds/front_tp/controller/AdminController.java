package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionDTO;
import ar.utn.ba.dds.front_tp.dto.colecciones.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.dto.admin.DashboardSummaryDTO;
import ar.utn.ba.dds.front_tp.services.ColeccionesApiService;
import ar.utn.ba.dds.front_tp.services.DashboardApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/admin") // Todas las rutas de admin empezarán con /admin
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

  private final ColeccionesApiService coleccionesApiService;
  private final DashboardApiService dashboardApiService;

  // --- GET para la página de gestión de colecciones ---
  @GetMapping("/colecciones")
  public String gestionarColecciones(Model model) {
    // Obtenemos la lista de colecciones para mostrar en la tabla
    List<ColeccionDTO> colecciones = coleccionesApiService.obtenerColecciones();
    model.addAttribute("colecciones", colecciones);

    // Preparamos un DTO vacío para el formulario de "Crear Nueva Colección"
    if (!model.containsAttribute("coleccionNueva")) {
      model.addAttribute("coleccionNueva", new ColeccionInputDTO());
    }

    return "admin-colecciones"; // Renderiza la vista admin-colecciones.html
  }

  // --- POST para crear una nueva colección ---
  @PostMapping("/colecciones/crear")
  public String crearColeccion(@ModelAttribute("coleccionNueva") ColeccionInputDTO coleccionInput,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

    // Obtenemos el token de la sesión
    AuthResponseDTO authData = (AuthResponseDTO) session.getAttribute("AUTH_DATA");
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
  public String mostrarDashboard(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
    // Verifico la sesión y obtengo el token
    AuthResponseDTO authData = (AuthResponseDTO) session.getAttribute("AUTH_DATA");
    if (authData == null || authData.getAccessToken() == null) {
      redirectAttributes.addFlashAttribute("error", "Tu sesión ha expirado.");
      return "redirect:/auth/login";
    }
    // Llamo al servicio para obtener los datos del resumen
    DashboardSummaryDTO summary = dashboardApiService.getSummary(authData.getAccessToken());

    // Paso los datos al modelo
    model.addAttribute("summary", summary);

    return "admin-dashboard"; // Renderiza la vista admin-dashboard.html
  }
}
