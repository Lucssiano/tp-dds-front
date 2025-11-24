package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.usuarios.UsuarioDTO;
import ar.utn.ba.dds.front_tp.services.GestionUsuariosApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  private final GestionUsuariosApiService gestionUsuariosApiService;

  @Autowired
  public AuthController(GestionUsuariosApiService gestionUsuariosApiService) {
    this.gestionUsuariosApiService = gestionUsuariosApiService;
  }

  // GET para Login (ruta por defecto o /login)
  @GetMapping(value = {"", "/login"})
  public String mostrarLogin(Model model) {
    return configurarVistaAuth(model, false); // false = modo login
  }

  // GET para Registro
  @GetMapping("/registrar")
  public String mostrarRegistro(Model model) {
    return configurarVistaAuth(model, true); // true = modo registro
  }

  // Método auxiliar para no repetir código
  private String configurarVistaAuth(Model model, boolean modoRegistro) {
    model.addAttribute("modoRegistro", modoRegistro);
    model.addAttribute("usuarioLogin", new UsuarioDTO());
    model.addAttribute("usuarioRegistro", new UsuarioDTO());
    return "usuarios/auth";
  }

  // POST Login
  @PostMapping("/login")
  public String login(@ModelAttribute("usuarioLogin") UsuarioDTO usuarioDTO, Model model) {
    // Normalmente Spring Security maneja esto. Si usas custom provider, está bien.
    return "redirect:/home";
  }

  // POST Registro
  @PostMapping("/registrar")
  public String registrar(@ModelAttribute("usuarioRegistro") UsuarioDTO usuarioDTO, Model model) {
    try {
      gestionUsuariosApiService.registrarUsuario(usuarioDTO);
      model.addAttribute("mensajeRegistro", "Usuario registrado correctamente. Ya podés iniciar sesión.");
      // Si sale bien, mostramos el login
      return configurarVistaAuth(model, false);
    } catch (Exception e) {
      log.error("Error al registrar usuario: {}", e.getMessage());
      model.addAttribute("errorRegistro", e.getMessage());
      // Si sale mal, nos quedamos en el registro mostrando el error
      return configurarVistaAuth(model, true);
    }
  }
}