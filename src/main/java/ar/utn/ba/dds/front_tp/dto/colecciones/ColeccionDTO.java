package ar.utn.ba.dds.front_tp.dto.colecciones;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class ColeccionDTO implements Serializable {
  private Long id;
  private String titulo;
  private String descripcion;
  private List<CriterioDePertenenciaInputDTO> criteriosDePertenencias;
  private List<String> fuentes;
  private TipoAlgoritmoConsenso algoritmoConsenso;

  // Campo para compatibilidad con el HTML. Usaremos una imagen de placeholder.
  private String imagenUrl = "https://picsum.photos/300/200?random=1";
}