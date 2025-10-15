package ar.utn.ba.dds.front_tp.dto.colecciones;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ColeccionInputDTO {
  private String titulo;
  private String descripcion;
  private List<CriterioDePertenenciaInputDTO> criteriosDePertenencias;
  private List<String> fuentes;
  private TipoAlgoritmoConsenso algoritmoConsenso;
}