package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.dto.input.ColeccionInputDTO;
import ar.utn.ba.dds.front_tp.services.ColeccionesApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/colecciones")
@RequiredArgsConstructor
public class ColeccionesController {

  private final ColeccionesApiService coleccionesApiService;

  @GetMapping
  public String mostrarColecciones(Model model) {
    // Llamo al servicio para obtener la lista de colecciones
    List<ColeccionInputDTO> colecciones = coleccionesApiService.obtenerColecciones();

    // Agrego la lista al modelo para que la vista pueda usarla
    model.addAttribute("colecciones", colecciones);

    // Devuelvo el nombre del archivo HTML que debe renderizarse
    return "colecciones";
  }
}
