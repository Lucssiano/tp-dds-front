package ar.utn.ba.dds.front_tp.exceptions;

public class DuplicateTitleException extends RuntimeException{
  /**
   * Excepción lanzada cuando se intenta crear un Hecho con un título que ya existe.
   * * @param message El mensaje que describe la causa específica del error.
   */
  public DuplicateTitleException(String message) {
    super(message);
  }

  /**
   * Constructor que permite envolver otra excepción original (causa).
   * * @param message El mensaje que describe la causa específica del error.
   * @param cause La causa original del error.
   */
  public DuplicateTitleException(String message, Throwable cause) {
    super(message, cause);
  }
}
