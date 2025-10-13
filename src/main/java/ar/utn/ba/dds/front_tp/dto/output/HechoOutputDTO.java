package ar.utn.ba.dds.front_tp.dto.output;

import ar.utn.ba.dds.front_tp.dto.hechos.UbicacionDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import lombok.Data;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HechoOutputDTO {
  private String titulo;
  private String descripcion;
  private String categoria;
  private Path multimedia;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate fecha;
  private BigDecimal latitud;
  private BigDecimal longitud;
  @JsonIgnore
  private List<String> etiquetas;
}
