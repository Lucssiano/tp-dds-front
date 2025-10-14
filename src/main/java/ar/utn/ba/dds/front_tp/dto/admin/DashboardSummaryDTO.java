package ar.utn.ba.dds.front_tp.dto.admin;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
public class DashboardSummaryDTO implements Serializable {
  private long hechosPendientes;
  private long solicitudesEliminacion;
  private long coleccionesActivas;
}