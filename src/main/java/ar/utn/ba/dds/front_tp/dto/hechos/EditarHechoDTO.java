package ar.utn.ba.dds.front_tp.dto.hechos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Builder
@Data
public class EditarHechoDTO {
  private String titulo;
  private String descripcion;
  private String categoria;
  private Path multimedia;
  private BigDecimal latitud;
  private BigDecimal longitud;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime fecha;
  private LocalDateTime fechaCarga;
  private String fuenteId;
  private String estado;
}
