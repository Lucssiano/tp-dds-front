package ar.utn.ba.dds.front_tp.dto.input;

import ar.utn.ba.dds.front_tp.dto.TipoCriterio;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CriterioDePertenenciaInputDTO {
  private Long id;
  private String nombreCriterio;
  private TipoCriterio tipoCriterio;
  private Map<String, Object> parametros = new HashMap<>();
}
