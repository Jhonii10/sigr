package co.sigr.common;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorDto(String mensaje, Map<String, String> campos, LocalDateTime fecha) {

  public static ErrorDto de(String mensaje) {
    return new ErrorDto(mensaje, Map.of(), LocalDateTime.now());
  }
}
