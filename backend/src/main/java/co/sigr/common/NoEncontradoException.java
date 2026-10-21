package co.sigr.common;

/** Recurso inexistente: se responde con HTTP 404. */
public class NoEncontradoException extends RuntimeException {

  public NoEncontradoException(String recurso, Object id) {
    super(recurso + " con id " + id + " no existe");
  }
}
