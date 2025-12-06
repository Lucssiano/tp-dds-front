package ar.utn.ba.dds.front_tp.dto.output;

import java.util.Map;
import ar.utn.ba.dds.front_tp.dto.TipoCriterio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriterioDePertenenciaOutputDTO {
  private Long id; // Si viene null es porq es un criterio nuevo

  private String nombreCriterio;

  private TipoCriterio tipoCriterio;

  private Map<String, Object> parametros;
}
