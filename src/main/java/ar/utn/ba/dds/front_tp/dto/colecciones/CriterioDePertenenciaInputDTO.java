package ar.utn.ba.dds.front_tp.dto.colecciones;

import lombok.Data;

import java.util.Map;

@Data
public class CriterioDePertenenciaInputDTO {
  private String nombreCriterio;
  private TipoCriterio tipoCriterio;
  private Map<String, Object> parametros;
}
