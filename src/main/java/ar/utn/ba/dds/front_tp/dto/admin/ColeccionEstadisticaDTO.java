package ar.utn.ba.dds.front_tp.dto.admin;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColeccionEstadisticaDTO {
    String titulo;

    String provincia;

    Long cantidad;

    LocalDateTime fechaCreacion;
}
