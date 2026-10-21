package co.sigr.common;

/** Error de regla de negocio: se responde con HTTP 400. */
public class NegocioException extends RuntimeException {

  public NegocioException(String mensaje) {
    super(mensaje);
  }
}
