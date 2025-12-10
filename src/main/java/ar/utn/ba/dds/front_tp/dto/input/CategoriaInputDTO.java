package ar.utn.ba.dds.front_tp.dto.input;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoriaInputDTO {
     Long id;
     String nombre;
}
