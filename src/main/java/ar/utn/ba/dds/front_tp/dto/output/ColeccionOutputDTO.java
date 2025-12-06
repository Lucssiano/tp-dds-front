package ar.utn.ba.dds.front_tp.dto.output;

import ar.utn.ba.dds.front_tp.dto.TipoAlgoritmoConsenso;
import lombok.Data;

import java.util.List;

@Data
public class ColeccionOutputDTO {
  private String titulo;

  private String descripcion;

  private List<CriterioDePertenenciaOutputDTO> criteriosDePertenencias;

  private List<Long> fuentesIds;

  private TipoAlgoritmoConsenso algoritmoConsenso;
}
