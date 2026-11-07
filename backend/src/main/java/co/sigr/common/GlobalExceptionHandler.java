package co.sigr.common;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NegocioException.class)
  public ResponseEntity<ErrorDto> negocio(NegocioException ex) {
    return ResponseEntity.badRequest().body(ErrorDto.de(ex.getMessage()));
  }

  @ExceptionHandler(NoEncontradoException.class)
  public ResponseEntity<ErrorDto> noEncontrado(NoEncontradoException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorDto.de(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorDto> validacion(MethodArgumentNotValidException ex) {
    Map<String, String> campos = new LinkedHashMap<>();
    ex.getBindingResult().getFieldErrors()
        .forEach(e -> campos.put(e.getField(), e.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(new ErrorDto("Datos inválidos", campos, LocalDateTime.now()));
  }

  @ExceptionHandler({HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
  public ResponseEntity<ErrorDto> solicitudMalformada(Exception ex) {
    return ResponseEntity.badRequest()
        .body(ErrorDto.de("Solicitud inválida: revise el formato de los datos enviados"));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorDto> archivoMuyGrande(MaxUploadSizeExceededException ex) {
    return ResponseEntity.badRequest().body(ErrorDto.de("La imagen no puede superar 5 MB"));
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ErrorDto> archivoFaltante(MissingServletRequestPartException ex) {
    return ResponseEntity.badRequest().body(ErrorDto.de("Seleccione una imagen"));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorDto> rutaInexistente(NoResourceFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorDto.de("Recurso no encontrado"));
  }

  @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
  public ResponseEntity<ErrorDto> credenciales(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ErrorDto.de("Credenciales inválidas o usuario inactivo"));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorDto> accesoDenegado(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorDto.de("No tiene permisos para esta operación"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDto> general(Exception ex) {
    LOG.error("Error no controlado", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorDto.de("Error interno del servidor"));
  }
}
