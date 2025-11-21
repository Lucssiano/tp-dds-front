package ar.utn.ba.dds.front_tp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuenteDTO implements Serializable {
  private Long id;
  private String nombre;

  // Tipo: "Estática", "Dinámica", "Proxy"
  private String tipo;

  // Para la columna "Colección Asociada" (si aplica)
  private String coleccionNombre;

  // Para la columna "URL/Datos"
  private String url;
}
