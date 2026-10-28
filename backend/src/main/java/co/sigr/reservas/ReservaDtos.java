package co.sigr.reservas;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public final class ReservaDtos {

  private ReservaDtos() {
  }

  public record CrearReservaRequest(
      @NotNull @FutureOrPresent LocalDate fecha,
      @NotNull @JsonFormat(pattern = "HH:mm") LocalTime hora,
      @NotNull @Min(1) @Max(20) Integer personas,
      Long mesaId) {
  }

  public record ReservaDto(Long id, String cliente, Integer mesa, Integer capacidadMesa,
                           LocalDate fecha, @JsonFormat(pattern = "HH:mm") LocalTime hora,
                           Integer personas, EstadoReserva estado) {

    public static ReservaDto de(Reserva r) {
      return new ReservaDto(r.getId(), r.getCliente().getNombre(), r.getMesa().getNumero(),
          r.getMesa().getCapacidad(), r.getFecha(), r.getHora(), r.getPersonas(), r.getEstado());
    }
  }
}
