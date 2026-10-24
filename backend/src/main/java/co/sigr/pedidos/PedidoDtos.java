package co.sigr.pedidos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PedidoDtos {

  private PedidoDtos() {
  }

  public record ItemRequest(@NotNull Long platoId, @NotNull @Min(1) @Max(50) Integer cantidad) {
  }

  public record CrearPedidoRequest(
      @NotNull Long mesaId,
      @NotEmpty List<@Valid ItemRequest> items,
      @Size(max = 255) String nota) {
  }

  public record CambioEstadoRequest(@NotNull EstadoPedido estado) {
  }

  public record DetalleDto(Long platoId, String plato, Integer cantidad,
                           BigDecimal precioUnitario, BigDecimal subtotal) {

    public static DetalleDto de(DetallePedido d) {
      return new DetalleDto(d.getPlato().getId(), d.getPlato().getNombre(), d.getCantidad(),
          d.getPrecioUnitario(),
          d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())));
    }
  }

  public record PedidoDto(Long id, Integer mesa, String mesero, EstadoPedido estado,
                          BigDecimal total, String nota, LocalDateTime creadoEn,
                          List<DetalleDto> detalles) {

    public static PedidoDto de(Pedido p) {
      return new PedidoDto(p.getId(), p.getMesa().getNumero(), p.getMesero().getNombre(),
          p.getEstado(), p.getTotal(), p.getNota(), p.getCreadoEn(),
          p.getDetalles().stream().map(DetalleDto::de).toList());
    }
  }
}
