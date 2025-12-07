package ar.utn.ba.dds.front_tp.dto.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaOutputDTO {
  private Long id;
  private String nombre;
}