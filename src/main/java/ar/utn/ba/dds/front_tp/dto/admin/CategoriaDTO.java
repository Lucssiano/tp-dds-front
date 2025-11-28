package ar.utn.ba.dds.front_tp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaDTO {
    String categoria;
    Long cantidad;
    String provincia;
    Integer hora;
    LocalDateTime fecha;
}
