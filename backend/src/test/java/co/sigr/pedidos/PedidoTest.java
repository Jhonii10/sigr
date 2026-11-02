package co.sigr.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import co.sigr.menu.Plato;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PedidoTest {

  private Plato plato(String precio) {
    Plato p = new Plato();
    p.setPrecio(new BigDecimal(precio));
    return p;
  }

  @Test
  void calculaTotalConPrecioCongelado() {
    Pedido pedido = new Pedido();
    Plato sancocho = plato("28000");
    pedido.agregarDetalle(new DetallePedido(sancocho, 2));
    pedido.agregarDetalle(new DetallePedido(plato("8000"), 3));
    sancocho.setPrecio(new BigDecimal("99999"));

    pedido.recalcularTotal();

    assertThat(pedido.getTotal()).isEqualByComparingTo("80000");
    assertThat(pedido.getDetalles()).allMatch(d -> d.getPedido() == pedido);
  }
}
