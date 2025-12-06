package ar.utn.ba.dds.front_tp.dto.input;

import ar.utn.ba.dds.front_tp.dto.hechos.UbicacionDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HechoInputDTO {
  private Long id;
  private String titulo;
  private String descripcion;
  private String categoria;
  private List<FuenteInputDTO> fuente;
  private List<String> multimedia;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime fechaHecho;
  private UbicacionDTO ubicacionOutputDTO;
  private List<String> etiquetas; // no me llegan parece
  private String usuario;
}
