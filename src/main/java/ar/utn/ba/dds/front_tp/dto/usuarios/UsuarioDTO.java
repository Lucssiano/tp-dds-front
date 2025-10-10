package ar.utn.ba.dds.front_tp.dto.usuarios;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
  private Long id;
  private String nombre;
  private String email;
  private String contrasena;
}
