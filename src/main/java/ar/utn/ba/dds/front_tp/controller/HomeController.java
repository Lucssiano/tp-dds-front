package ar.utn.ba.dds.front_tp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
  @GetMapping({"/", "/home"})
  public String mostrarHome(Model model) {

    // Datos simulados para mostrar dinámicamente
    var coleccionesDestacadas = List.of(
        Map.of("id", 1, "titulo", "Rutas de la Revolución",
            "descripcion", "Un recorrido por los lugares clave de la independencia de Latinoamérica.",
            "imagenUrl", "https://picsum.photos/300/200?random=1"),
        Map.of("id", 2, "titulo", "Eventos Naturales Históricos",
            "descripcion", "Terremotos, erupciones y fenómenos que cambiaron el curso de la historia.",
            "imagenUrl", "https://picsum.photos/300/200?random=2")
    );

    var hechoDestacado = Map.of(
        "id", 3,
        "titulo", "La Batalla de Boyacá",
        "descripcion", "El 7 de agosto de 1819 se libró una de las batallas más decisivas de la independencia.",
        "imagenUrl", "https://picsum.photos/600/400?random=3"
    );

    model.addAttribute("coleccionesDestacadas", coleccionesDestacadas);
    model.addAttribute("hechoDestacado", hechoDestacado);

    return "home"; // templates/home.html
  }
}
