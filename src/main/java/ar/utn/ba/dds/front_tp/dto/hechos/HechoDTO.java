package ar.utn.ba.dds.front_tp.dto.hechos;

import ar.utn.ba.dds.front_tp.dto.output.FuenteOutputDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HechoDTO {
  private Long id;
  private String titulo;
  private String descripcion;
  private String categoria;
  private List<FuenteOutputDTO> fuente;
  private List<String> multimedia;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime fechaHecho;
  private UbicacionDTO ubicacionOutputDTO;
  private List<String> etiquetas; // no me llegan parece
  private String usuario;
}
