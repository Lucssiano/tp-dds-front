package ar.utn.ba.dds.front_tp.dto.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionInputDTO {
  private String provincia;
  private String municipio;
  private String departamento;
  private BigDecimal latitud;
  private BigDecimal longitud;
}
