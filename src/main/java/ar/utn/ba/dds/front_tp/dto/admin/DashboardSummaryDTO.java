package ar.utn.ba.dds.front_tp.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
@Builder
public class DashboardSummaryDTO implements Serializable {
  private long hechosPendientes;
  private long solicitudesEliminacion;
  private long solicitudesModificacion;
  private long coleccionesActivas;
}