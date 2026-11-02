package co.sigr.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoPedidoTest {

  @Test
  void flujoNormalDelPedido() {
    assertThat(EstadoPedido.RECIBIDO.puedeCambiarA(EstadoPedido.EN_PREPARACION)).isTrue();
    assertThat(EstadoPedido.EN_PREPARACION.puedeCambiarA(EstadoPedido.LISTO)).isTrue();
    assertThat(EstadoPedido.LISTO.puedeCambiarA(EstadoPedido.ENTREGADO)).isTrue();
  }

  @Test
  void noPermiteSaltosNiRetrocesos() {
    assertThat(EstadoPedido.RECIBIDO.puedeCambiarA(EstadoPedido.LISTO)).isFalse();
    assertThat(EstadoPedido.LISTO.puedeCambiarA(EstadoPedido.RECIBIDO)).isFalse();
    assertThat(EstadoPedido.ENTREGADO.puedeCambiarA(EstadoPedido.CANCELADO)).isFalse();
  }

  @Test
  void pagadoSoloLoAsignaCaja() {
    for (EstadoPedido e : EstadoPedido.values()) {
      assertThat(e.puedeCambiarA(EstadoPedido.PAGADO)).isFalse();
    }
  }

  @Test
  void pedidosActivos() {
    assertThat(EstadoPedido.LISTO.esActivo()).isTrue();
    assertThat(EstadoPedido.PAGADO.esActivo()).isFalse();
    assertThat(EstadoPedido.CANCELADO.esActivo()).isFalse();
  }
}
