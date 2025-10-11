package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.services.GestionUsuariosApiService;
import ar.utn.ba.dds.front_tp.services.HechosApiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/hechos")
@RequiredArgsConstructor
public class HechosController {
  private static final Logger log = LoggerFactory.getLogger(HechosController.class);
  private final HechosApiService hechosApiService;

  @GetMapping("/mapa")
  public String mostrarMapa(Model model) {
    log.info(">>> Entrando a /hechos/mapa");
    try {
      List<HechoDTO> hechos = hechosApiService.obtenerHechos();
      log.info("Cantidad de hechos recibidos: {}", hechos.size());
      model.addAttribute("hechos", hechos);
    } catch (Exception e) {
      log.error("Error al obtener hechos", e);
      model.addAttribute("error", "Error al obtener hechos: " + e.getMessage());
    }
    return "mapa";
  }
}
