package ar.utn.ba.dds.front_tp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorViewController {

  // Este es el método que falta para romper el bucle
  @GetMapping("/404")
  public String notFoundPage() {
    return "404"; // Esto busca el archivo 404.html en templates
  }

  // Ya que estamos, agrega el del 403 y 500 si los usas
  @GetMapping("/403")
  public String accessDeniedPage() {
    return "403";
  }
}

