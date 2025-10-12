package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.UsuarioDTO;
import ar.utn.ba.dds.front_tp.services.GestionUsuariosApiService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  @Autowired
  private final GestionUsuariosApiService gestionUsuariosApiService;
  @Autowired
  private HttpSession session;

  public AuthController(GestionUsuariosApiService gestionUsuariosApiService) {
    this.gestionUsuariosApiService = gestionUsuariosApiService;
  }

  @GetMapping()
  public String mostrarAuth(Model model,
                            @RequestParam(value = "modoRegistro", required = false, defaultValue = "false") boolean modoRegistro) {

    model.addAttribute("modoRegistro", modoRegistro);
    model.addAttribute("usuarioLogin", new UsuarioDTO());
    model.addAttribute("usuarioRegistro", new UsuarioDTO());
    return "usuarios/auth";
  }

  @PostMapping("/login")
  public String login(@ModelAttribute("usuarioLogin") UsuarioDTO usuarioDTO, Model model) {
    try {
      AuthResponseDTO token = gestionUsuariosApiService.login(usuarioDTO.getEmail(), usuarioDTO.getContrasena());
      session.setAttribute("AUTH_DATA", token);
      model.addAttribute("mensajeLogin", "Inicio de sesión exitoso");
      return "redirect:/";
    } catch (Exception e) {
      model.addAttribute("errorLogin", e.getMessage());
      return mostrarAuth(model, false);
    }
  }

  @PostMapping("/registrar")
  public String registrar(@ModelAttribute("usuarioRegistro") UsuarioDTO usuarioDTO, Model model) {
    try {
      gestionUsuariosApiService.registrarUsuario(usuarioDTO);
      model.addAttribute("mensajeRegistro", "Usuario registrado correctamente. Ya podés iniciar sesión.");
      return mostrarAuth(model, false);
    } catch (Exception e) {
      log.error("Error al registrar usuario: {}", e.getMessage());
      model.addAttribute("errorRegistro", e.getMessage());
      return mostrarAuth(model, true);
    }
  }

}
