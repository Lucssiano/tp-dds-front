package ar.utn.ba.dds.front_tp.dto.usuarios;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO implements Serializable {
  private String accessToken;
  private String refreshToken;
  private String rol;
}
