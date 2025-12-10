package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.exceptions.api.ApiException;
import ar.utn.ba.dds.front_tp.exceptions.api.AutenticationException;
import ar.utn.ba.dds.front_tp.exceptions.api.AuthorizationException;
import ar.utn.ba.dds.front_tp.exceptions.api.GeneralApiException;
import ar.utn.ba.dds.front_tp.exceptions.api.InternalServerErrorException;
import ar.utn.ba.dds.front_tp.exceptions.api.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandlerController {

  // ==========================================
  // 1. AUTENTICACIÓN (401)
  // ==========================================
  // El usuario no está logueado o el token venció.
  @ExceptionHandler(AutenticationException.class)
  public String handleAuthError(AutenticationException ex, RedirectAttributes redirectAttributes) {
    log.warn("⚠️ Sesión inválida o expirada: {}", ex.getMessage());

    // Intentamos sacar el mensaje del Backend, si no, usamos uno genérico
    String mensaje = (ex.getApiError() != null && ex.getApiError().message() != null)
        ? ex.getApiError().message()
        : "Tu sesión ha expirado. Por favor, ingresa nuevamente.";

    redirectAttributes.addFlashAttribute("error", mensaje);
    return "redirect:/auth/login";
  }

  // ==========================================
  // 2. AUTORIZACIÓN (403)
  // ==========================================
  // Agregamos AccessDeniedException.class para atrapar los @PreAuthorize fallidos
  @ExceptionHandler({AuthorizationException.class, AccessDeniedException.class})
  public String handleAuthzError(Exception ex, RedirectAttributes redirectAttributes) {
    // Nota: cambié el argumento a Exception para que acepte ambas clases
    log.error("⛔ Acceso denegado (403): {}", ex.getMessage());

    return "redirect:/403"; // Asegúrate de que esta vista exista
  }

  // ==========================================
  // 3. RECURSO NO ENCONTRADO (404)
  // ==========================================
  // ID incorrecto, URL mal escrita, recurso borrado.
  @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
  public String handleNotFound(Exception ex, RedirectAttributes redirectAttributes) {
    log.warn("🔍 Recurso no encontrado (404): {}", ex.getMessage());

    // 1. Mensaje por defecto (para cuando sea error de Spring o URL mal escrita)
    String mensaje = "El recurso solicitado no fue encontrado.";

    // 2. Si es TU excepción, intentamos rescatar el mensaje del Backend
    if (ex instanceof ResourceNotFoundException myEx) {
      if (myEx.getApiError() != null && myEx.getApiError().message() != null) {
        mensaje = myEx.getApiError().message();
      }
    }

    redirectAttributes.addFlashAttribute("error", mensaje);

    return "redirect:/404";
  }

  // ==========================================
  // 4. ERRORES DE API GENÉRICOS (La "Cubeta Comodín")
  // ==========================================
  // Atrapa: InternalServerError (500), Conflict (409), Teapot (418), y cualquier GeneralApiException.
  @ExceptionHandler({InternalServerErrorException.class, GeneralApiException.class})
  public String handleApiErrors(ApiException ex, RedirectAttributes redirectAttributes) {
    log.error("🔥 Error de API (Status {}): {}", ex.getStatus(), ex.getApiError());

    // Extraemos el mensaje de negocio del ApiError (Ej: "El nombre ya existe")
    String msgBackend = (ex.getApiError() != null && ex.getApiError().message() != null)
        ? ex.getApiError().message()
        : "Error de comunicación con el servidor.";

    redirectAttributes.addFlashAttribute("error", "Error del sistema: " + msgBackend);

    return "redirect:/home";
  }

  // ==========================================
  // 5. ERRORES INESPERADOS (Java / Cliente)
  // ==========================================
  // NullPointer, Timeout local, errores de renderizado de Thymeleaf, etc.
  @ExceptionHandler(Exception.class)
  public String handleUnexpected(Exception ex, RedirectAttributes redirectAttributes) {
    log.error("💀 Excepción inesperada no controlada en cliente: ", ex);

    redirectAttributes.addFlashAttribute("error", "Ocurrió un error inesperado en la aplicación. Contacte soporte.");

    return "redirect:/home";
  }
}