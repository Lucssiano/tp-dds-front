package ar.utn.ba.dds.front_tp.dto.hechos;

import ar.utn.ba.dds.front_tp.dto.usuarios.Permiso;
import ar.utn.ba.dds.front_tp.dto.usuarios.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolesPermisosDTO {
  private String username;
  private Rol rol;
  private List<Permiso> permisos;
}
