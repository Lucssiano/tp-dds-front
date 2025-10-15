package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.dto.hechos.RolesPermisosDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.UsuarioDTO;
import ar.utn.ba.dds.front_tp.exceptions.NotFoundException;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class GestionUsuariosApiService {

  private static final Logger log = LoggerFactory.getLogger(GestionUsuariosApiService.class);
  private final WebClient webClient;
  private final WebApiCallerService webApiCallerService;
  private final String authServiceUrl;
  private final String usuariosServiceUrl;

  @Autowired
  public GestionUsuariosApiService(
      WebApiCallerService webApiCallerService,
      @Value("${auth.service.url}") String authServiceUrl,
      @Value("${usuarios.service.url}") String usuariosServiceUrl) {
    this.webClient = WebClient.builder().build();
    this.webApiCallerService = webApiCallerService;
    this.authServiceUrl = authServiceUrl;
    this.usuariosServiceUrl = usuariosServiceUrl;
  }

  public AuthResponseDTO login(String email, String password){
    try {
      AuthResponseDTO response = webClient
          .post()
          .uri(authServiceUrl + "/auth")
          .bodyValue(Map.of(
              "password", password,
              "email", email
          ))
          .retrieve()
          .bodyToMono(AuthResponseDTO.class)
          .block();
      return response;
    } catch (WebClientResponseException e){
      log.error(e.getMessage());
      if (e.getStatusCode()== HttpStatus.NOT_FOUND){
        //LOGIN FALLIDO - CREDENCIALES INCORRECTAS
        return null;
      }
      // Otros errores HTTP
      throw new RuntimeException("Error en el servicio de autenticación: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new RuntimeException("Error de conexión con el servicio de autenticación: " + e.getMessage(), e);
    }

  }

  public RolesPermisosDTO getRolesPermisos(String accestoken){
    try {
      return webApiCallerService.getWithAuth(
          authServiceUrl + "/auth/user/roles-permisos",
          accestoken,
          RolesPermisosDTO.class
      );

    } catch (Exception e) {
      log.error(e.getMessage());
      throw new RuntimeException("Error al obtener roles y permisos: " + e.getMessage(), e);
    }
  }

  public List<UsuarioDTO> obtenerTodosLosUsuario() {
    List<UsuarioDTO> response = webApiCallerService.getList(usuariosServiceUrl + "/usuarios", UsuarioDTO.class);
    return response != null ? response : List.of();
  }

//  public UsuarioDTO obtenerUsuarioPorId(Long id){
//    UsuarioDTO response = webApiCallerService.get(usuariosServiceUrl + "/usuarios/" + id, UsuarioDTO.class);
//    if (response == null) {
//      throw new NotFoundException("Usuario", id.toString());
//    }
//    return response;
//  }
//
//  public UsuarioDTO crearUsuario(UsuarioDTO usuarioDTO) {
//    UsuarioDTO response = webApiCallerService.post(usuariosServiceUrl + "/usuarios", usuarioDTO, UsuarioDTO.class);
//    if (response == null) {
//      throw new RuntimeException("Error al crear alumno en el servicio externo");
//    }
//    return response;
//  }
//
//  public UsuarioDTO actualizarUsuario(Long id, UsuarioDTO usuarioDTO) {
//    UsuarioDTO response = webApiCallerService.put(usuariosServiceUrl + "/usuarios/" + id, usuarioDTO, UsuarioDTO.class);
//    if (response == null) {
//      throw new RuntimeException("Error al actualizar usuario en el servicio externo");
//    }
//    return response;
//  }
//
//  public void eliminarUsuario(Long id) {
//    webApiCallerService.delete(usuariosServiceUrl + "/usuarios/" + id);
//  }
//
//  public boolean existeUsuario(Long id) {
//    try {
//      obtenerUsuarioPorId(id);
//      return true;
//    } catch (NotFoundException e) {
//      return false;
//    } catch (Exception e) {
//      throw new RuntimeException("Error al verificar existencia del usuario: " + e.getMessage(), e);
//    }
//  }

  public UsuarioDTO registrarUsuario(UsuarioDTO usuarioDTO) {
    try {
      UsuarioDTO response = webApiCallerService.post(
          authServiceUrl + "/auth/register",
          usuarioDTO,
          UsuarioDTO.class
      );

      if (response == null) {
        throw new RuntimeException("El servicio de registro no devolvió respuesta");
      }

      return response;

    } catch (WebClientResponseException e) {
      log.error("Error HTTP en registrarUsuario: {}", e.getMessage());

      if (e.getStatusCode() == HttpStatus.CONFLICT) {
        throw new RuntimeException("El usuario ya existe: " + e.getResponseBodyAsString());
      } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
        throw new RuntimeException("Datos inválidos al registrar usuario: " + e.getResponseBodyAsString());
      }

      throw new RuntimeException("Error en el servicio de registro: " + e.getMessage(), e);

    } catch (Exception e) {
      log.error("Error general en registrarUsuario: {}", e.getMessage());
      throw new RuntimeException("Error al registrar usuario: " + e.getMessage(), e);
    }
  }


}

