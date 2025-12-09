package ar.utn.ba.dds.front_tp.dto.input;

import ar.utn.ba.dds.front_tp.dto.output.EstadoSolicitud;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SolicitudModificacionInputDTO {
  private Long id;
  private Long hechoId;
  private String hechoTitulo;
  private String hechoDescripcion;
  private String hechoCategoria;
  private BigDecimal hechoLatitud;
  private BigDecimal hechoLongitud;
  private List<String> hechoMultimedia;
  private LocalDateTime fechaHecho;
  private EstadoSolicitud estado;
  private String usuario;
}
