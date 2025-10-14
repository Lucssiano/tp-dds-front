package ar.utn.ba.dds.front_tp.dto.colecciones;

import lombok.Data;
import java.io.Serializable;

@Data
public class ColeccionInputDTO implements Serializable {
  private String titulo;
  private String descripcion;
  // TODO: incluiR los campos complejos como criterios, fuentes, etc.
}