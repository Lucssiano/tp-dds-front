package ar.utn.ba.dds.front_tp.providers;

import ar.utn.ba.dds.front_tp.dto.hechos.RolesPermisosDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.UsuarioDTO;
import ar.utn.ba.dds.front_tp.services.GestionUsuariosApiService;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomAuthProvider implements AuthenticationProvider {
  private static final Logger log = LoggerFactory.getLogger(CustomAuthProvider.class);
  private final GestionUsuariosApiService externalAuthService;

  public CustomAuthProvider(GestionUsuariosApiService externalAuthService) {
    this.externalAuthService = externalAuthService;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String email = authentication.getName();
    String password = authentication.getCredentials().toString();

    try {
      // llama al servicio externo de autenticación (backend gestion-usuarios)
      AuthResponseDTO authResponse = externalAuthService.login(email, password);

      if (authResponse == null) {
        throw new BadCredentialsException("Usuario o contraseña inválidos");
      }

      log.info("Usuario logeado! Configurando variables de sesión");
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
      HttpServletRequest request = attributes.getRequest();

      request.getSession().setAttribute("accessToken", authResponse.getAccessToken());
      request.getSession().setAttribute("refreshToken", authResponse.getRefreshToken());
      request.getSession().setAttribute("email", email);

      log.info("Buscando roles y permisos del usuario");
      RolesPermisosDTO rolesPermisos = externalAuthService.getRolesPermisos(authResponse.getAccessToken());

      log.info("Cargando roles y permisos del usuario en sesión");
      request.getSession().setAttribute("rol", rolesPermisos.getRol());
      request.getSession().setAttribute("permisos", rolesPermisos.getPermisos());

      List<GrantedAuthority> authorities = new ArrayList<>();
      rolesPermisos.getPermisos().forEach(permiso -> {
        authorities.add(new SimpleGrantedAuthority(permiso.name()));
      });
      authorities.add(new SimpleGrantedAuthority("ROLE_" + rolesPermisos.getRol().name()));

      return new UsernamePasswordAuthenticationToken(email, password, authorities);
    } catch (Exception e) {
      log.error("Error de autenticación: {}", e.getMessage());
      throw new BadCredentialsException("Credenciales inválidas");
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }



}
