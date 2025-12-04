package ar.utn.ba.dds.front_tp.dto.output;

import ar.utn.ba.dds.front_tp.dto.hechos.UbicacionDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class HechoOutputDTO {
  private String titulo;
  private String descripcion;
  private String categoria;
  private List<String> multimedia;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
  private LocalDateTime fecha; // TODO: TIENE QUE SER LOCALDATETIME
  private BigDecimal latitud;
  private BigDecimal longitud;
  @JsonIgnore
  private List<String> etiquetas;
  private String usuario;
}
