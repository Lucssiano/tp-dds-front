package ar.utn.ba.dds.front_tp.dto.hechos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoriaDTO {
     Long id;
     String nombre;
}
