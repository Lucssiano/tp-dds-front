package ar.utn.ba.dds.front_tp.providers;

import ar.utn.ba.dds.front_tp.dto.hechos.RolesPermisosDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.services.GestionUsuariosApiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomAuthProvider implements AuthenticationProvider {
  private static final Logger log = LoggerFactory.getLogger(CustomAuthProvider.class);
  private final GestionUsuariosApiService externalAuthService;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String email = authentication.getName();
    String password = authentication.getCredentials().toString();
    log.info("Intentando login con email={} y password={}", email, password);

    try {
      // 1. Llama a los servicios del back-end
      AuthResponseDTO authResponse = externalAuthService.login(email, password);
      if (authResponse == null || authResponse.getAccessToken() == null) {
        throw new BadCredentialsException("Usuario o contraseña inválidos");
      }

      RolesPermisosDTO rolesPermisos = externalAuthService.getRolesPermisos(authResponse.getAccessToken());
      if (rolesPermisos == null || rolesPermisos.getRol() == null) {
        throw new BadCredentialsException("No se pudieron obtener los roles del usuario.");
      }

      // 2. Construye las authorities (rol + permisos)
      List<GrantedAuthority> authorities = new ArrayList<>();
      authorities.add(new SimpleGrantedAuthority("ROLE_" + rolesPermisos.getRol().name()));
      if (rolesPermisos.getPermisos() != null) {
        rolesPermisos.getPermisos().forEach(permiso -> {
          authorities.add(new SimpleGrantedAuthority(permiso.name()));
        });
      }

      // 3. Crea el token de autenticación de Spring
      UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password, authorities);

      // 4. ¡AQUÍ ESTÁ LA MAGIA! Adjuntamos nuestro ColeccionOutputDTO al token.
      // Spring se encargará de guardar este objeto completo en la sesión.
      authToken.setDetails(authResponse);

      return authToken;

    } catch (Exception e) {
      log.error("Error durante la autenticación para {}: {}", email, e.getMessage());
      throw new BadCredentialsException("Error en el servicio de autenticación.");
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}

