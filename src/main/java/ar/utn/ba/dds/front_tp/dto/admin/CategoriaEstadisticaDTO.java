package ar.utn.ba.dds.front_tp.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Builder
public class CategoriaEstadisticaDTO {
    String categoria;
    Long cantidad;
    String provincia;
    Integer hora;
    LocalDateTime fecha;
}
