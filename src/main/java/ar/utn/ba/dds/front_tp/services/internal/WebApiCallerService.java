package ar.utn.ba.dds.front_tp.services.internal;

import ar.utn.ba.dds.front_tp.dto.usuarios.AuthResponseDTO;
import ar.utn.ba.dds.front_tp.dto.usuarios.RefreshTokenDTO;
import ar.utn.ba.dds.front_tp.exceptions.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.util.List;

@Service
public class WebApiCallerService {
  private final WebClient webClient;
  private final String authServiceUrl;
  private static final Logger log = LoggerFactory. getLogger(WebApiCallerService.class);

  @Autowired
  private HttpSession session;


  public WebApiCallerService(@Value("${auth.service.url}") String authServiceUrl) {
    final int size = 16 * 1024 * 1024;
    final ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
        .build();

    this.webClient = WebClient.builder()
        .exchangeStrategies(strategies)
        .build();

    this.authServiceUrl = authServiceUrl;
  }

  // Dentro de WebApiCallerService.java

  public <T> List<T> getPublicList(String url, Class<T> responseType) {
    try {
      return webClient.get()
          .uri(url)
          .retrieve()
          .bodyToFlux(responseType)
          .collectList()
          .block();
    } catch (WebClientResponseException e) {
      log.error("Error en la llamada pública a la API ({}): {}", url, e.getMessage());
      return null; // O Collections.emptyList();
    }
  }

  /**
   * Realiza una petición POST a un endpoint protegido, incluyendo el token JWT.
   * @param url La URL del endpoint.
   * @param requestBody El objeto que se enviará en el cuerpo de la petición.
   * @param responseType La clase del objeto que se espera como respuesta.
   * @param token El token JWT para la autenticación.
   * @param <T> El tipo del objeto de respuesta.
   * @param <R> El tipo del objeto del cuerpo de la petición.
   * @return El objeto de respuesta deserializado.
   */
  public <T, R> T postWithAuth(String url, R requestBody, Class<T> responseType, String token) {
    log.info("Llamada POST AUTENTICADA a: {}", url);

    var requestSpec = webClient.post()
        .uri(url)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);

    // CORRECCIÓN: Solo agregamos el body si NO es null
    if (requestBody != null) {
      requestSpec.bodyValue(requestBody);
    }

    // Si esperamos Void (respuesta vacía), usamos toBodilessEntity para evitar errores de parsing
    if (responseType.equals(Void.class)) {
      requestSpec.retrieve().toBodilessEntity().block();
      return null;
    }

    return requestSpec.retrieve()
        .bodyToMono(responseType)
        .block();
  }


  /**
   * Ejecuta una llamada al API con manejo automático de refresh token
   * @param apiCall función que ejecuta la llamada al API
   * @return resultado de la llamada al API
   */
  public <T> T executeWithTokenRetry(ApiCall<T> apiCall) {
    String accessToken = getAccessTokenFromSession();
    String refreshToken = getRefreshTokenFromSession();

    if (accessToken == null) {
      throw new RuntimeException("No hay token de acceso disponible");
    }

    try {
      // Primer intento con el token actual
      return apiCall.execute(accessToken);
    } catch (WebClientResponseException e) {
      if ((e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) && refreshToken != null) {
        try {
          // Token expirado, intentar refresh
          AuthResponseDTO newTokens = refreshToken(refreshToken);

          // Segundo intento con el nuevo token
          return apiCall.execute(newTokens.getAccessToken());
        } catch (Exception refreshError) {
          throw new RuntimeException("Error al refrescar token y reintentar: " + refreshError.getMessage(), refreshError);
        }
      }
      if(e.getStatusCode() == HttpStatus.NOT_FOUND){
        throw new NotFoundException(e.getMessage());
      }
      throw new RuntimeException("Error en llamada al API: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new RuntimeException("Error de conexión con el servicio: " + e.getMessage(), e);
    }
  }

  /**
   * Ejecuta una llamada HTTP GET
   */
  public <T> T get(String url, Class<T> responseType) {
    return executeWithTokenRetry(accessToken ->
        webClient
            .get()
            .uri(url)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(responseType)
            .block()
    );
  }

  /**
   * Ejecuta una llamada HTTP GET que retorna una lista
   */
  public <T> java.util.List<T> getList(String url, Class<T> responseType) {
    return executeWithTokenRetry(accessToken ->
        webClient
            .get()
            .uri(url)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToFlux(responseType)
            .collectList()
            .block()
    );
  }

  /**
   * Ejecuta una llamada HTTP GET con un token específico (sin usar sesión)
   */
    public <T> T getWithAuth(String url, String accessToken, Class<T> responseType) {
      try {
        return webClient
            .get()
            .uri(url)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(responseType)
            .block();
      } catch (Exception e) {
        throw new RuntimeException("Error en llamada al API: " + e.getMessage(), e);
      }
    }

  /**
   * Ejecuta una llamada HTTP GET con un token específico y retorna una lista.
   */
  public <T> List<T> getListWithAuth(String url, String accessToken, Class<T> responseType) {
    try {
      return webClient
          .get()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .bodyToFlux(responseType)
          .collectList()
          .block();
    } catch (Exception e) {
      log.error("Error en getListWithAuth: " + e.getMessage());
      throw new RuntimeException("Error en llamada al API: " + e.getMessage(), e);
    }
  }

  /**
   * Ejecuta una llamada HTTP POST
   */
  public <T> T post(String url, Object body, Class<T> responseType) {
    // 🔹 Si es login o register → no requiere token
    if (url.contains("/auth/register") || url.endsWith("/auth") || url.contains("/auth/login")) {
      try {
        return webClient
            .post()
            .uri(url)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(responseType)
            .block();
      } catch (WebClientResponseException e) {
        throw new RuntimeException("Error HTTP en llamada pública: " + e.getMessage(), e);
      } catch (Exception e) {
        throw new RuntimeException("Error en llamada pública: " + e.getMessage(), e);
      }
    }

    // 🔹 Si no es público → usar el flujo normal con token y refresh
    return executeWithTokenRetry(accessToken ->
        webClient
            .post()
            .uri(url)
            .header("Authorization", "Bearer " + accessToken)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(responseType)
            .block()
    );
  }

  /**
   * Ejecuta una llamada HTTP PUT
   */
  public <T> T put(String url, Object body, Class<T> responseType) {
    return executeWithTokenRetry(accessToken ->
        webClient
            .put()
            .uri(url)
            .header("Authorization", "Bearer " + accessToken)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(responseType)
            .block()
    );
  }

  // En WebApiCallerService.java

  public <T, R> void putWithAuth(String url, R requestBody, Class<T> responseType, String token) {
    try {
      webClient.put()
          .uri(url)
          .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer " + token)
          .bodyValue(requestBody)
          .retrieve()
          .bodyToMono(responseType)
          .block();
    } catch (Exception e) {
      throw new RuntimeException("Error en PUT con Auth: " + e.getMessage(), e);
    }
  }

  /**
   * Ejecuta una llamada HTTP DELETE
   */
  public void delete(String url) {
    executeWithTokenRetry(accessToken -> {
      webClient
          .delete()
          .uri(url)
          .header("Authorization", "Bearer " + accessToken)
          .retrieve()
          .bodyToMono(Void.class)
          .block();
      return null;
    });
  }
  /**
   * Ejecuta una llamada HTTP DELETE con un token específico.
   */
  public void delete(String url, String token) {
    try {
      webClient
          .delete()
          .uri(url)
          .header("Authorization", "Bearer " + token)
          .retrieve()
          .bodyToMono(Void.class)
          .block();
    } catch (Exception e) {
      throw new RuntimeException("Error en llamada DELETE al API: " + e.getMessage(), e);
    }
  }

  /**
   * Refresca el access token usando el refresh token
   */
  private AuthResponseDTO refreshToken(String refreshToken) {
    try {
      RefreshTokenDTO refreshRequest = RefreshTokenDTO.builder()
          .refreshToken(refreshToken)
          .build();

      AuthResponseDTO response = webClient
          .post()
          .uri(authServiceUrl + "/auth/refresh")
          .bodyValue(refreshRequest)
          .retrieve()
          .bodyToMono(AuthResponseDTO.class)
          .block();

      // Actualizar tokens en sesión
      updateTokensInSession(response.getAccessToken(), response.getRefreshToken());
      return response;
    } catch (Exception e) {
      throw new RuntimeException("Error al refrescar token: " + e.getMessage(), e);
    }
  }

  /**
   * Obtiene el access token de la sesión
   */
  private String getAccessTokenFromSession() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpServletRequest request = attributes.getRequest();
    return (String) request.getSession().getAttribute("accessToken");
  }

  /**
   * Obtiene el refresh token de la sesión
   */
  private String getRefreshTokenFromSession() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpServletRequest request = attributes.getRequest();
    return (String) request.getSession().getAttribute("refreshToken");
  }

  /**
   * Actualiza los tokens en la sesión
   */
  private void updateTokensInSession(String accessToken, String refreshToken) {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpServletRequest request = attributes.getRequest();

    request.getSession().setAttribute("accessToken", accessToken);
    request.getSession().setAttribute("refreshToken", refreshToken);
  }

  /**
   * Interfaz funcional para ejecutar llamadas al API con token
   */
  @FunctionalInterface
  public interface ApiCall<T> {
    T execute(String accessToken) throws Exception;
  }

}
