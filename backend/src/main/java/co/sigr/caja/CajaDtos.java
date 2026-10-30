package co.sigr.caja;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class CajaDtos {

  private CajaDtos() {
  }

  public record AperturaRequest(@NotNull @DecimalMin("0.0") BigDecimal montoApertura) {
  }

  public record PagoRequest(@NotNull Long pedidoId, @NotNull MedioPago medioPago) {
  }

  public record PagoDto(Long id, Long pedidoId, Integer mesa, MedioPago medioPago,
                        BigDecimal valor, LocalDateTime creadoEn) {

    public static PagoDto de(Pago p) {
      return new PagoDto(p.getId(), p.getPedido().getId(), p.getPedido().getMesa().getNumero(),
          p.getMedioPago(), p.getValor(), p.getCreadoEn());
    }
  }

  public record ResumenCajaDto(Long id, LocalDate fecha, EstadoCaja estado,
                               BigDecimal montoApertura, BigDecimal totalVentas,
                               Map<MedioPago, BigDecimal> totalesPorMedio,
                               BigDecimal efectivoEsperado, BigDecimal montoCierre,
                               LocalDateTime abiertaEn, LocalDateTime cerradaEn,
                               List<PagoDto> pagos) {
  }
}
