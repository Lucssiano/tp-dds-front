package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.hechos.HechoDTO;
import ar.utn.ba.dds.front_tp.services.HechosApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/hechos")
@RequiredArgsConstructor
public class HechosController {
  private static final Logger log = LoggerFactory.getLogger(HechosController.class);
  private final HechosApiService hechosApiService;

  // Inyectamos el conversor de JSON
  private final ObjectMapper objectMapper;

  @GetMapping("/mapa")
  public String mostrarMapa(Model model) {
    log.info(">>> Entrando a /hechos/mapa");
    try {
      List<HechoDTO> hechos = hechosApiService.obtenerHechos();
      log.info("Cantidad de hechos recibidos: {}", hechos.size());

      // Convertimos la lista a un String JSON
      String hechosJson = objectMapper.writeValueAsString(hechos);

      // Pasamos el STRING JSON al modelo
      model.addAttribute("hechosJson", hechosJson);

    } catch (Exception e) {
      log.error("Error al obtener hechos o al convertirlos a JSON", e);
      model.addAttribute("hechosJson", "[]"); // Pasamos un array vacío en caso de error
    }
    return "mapa";
  }
}