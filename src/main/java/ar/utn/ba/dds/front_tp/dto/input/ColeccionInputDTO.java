package ar.utn.ba.dds.front_tp.dto.input;

import ar.utn.ba.dds.front_tp.dto.TipoAlgoritmoConsenso;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ColeccionInputDTO {
  private Long id;
  private String titulo;
  private String descripcion;
  private List<CriterioDePertenenciaInputDTO> criteriosDePertenencias = new ArrayList<>();
  private List<FuenteInputDTO> fuentes = new ArrayList<>();
  private TipoAlgoritmoConsenso algoritmoConsenso;
  private String imagenUrl = "https://picsum.photos/300/200?random=1";
}