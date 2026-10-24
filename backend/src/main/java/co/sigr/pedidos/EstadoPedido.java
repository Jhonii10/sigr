package co.sigr.pedidos;

import java.util.Map;
import java.util.Set;

public enum EstadoPedido {
  RECIBIDO, EN_PREPARACION, LISTO, ENTREGADO, PAGADO, CANCELADO;

  /** Transiciones permitidas desde el tablero. PAGADO solo lo asigna el módulo de caja. */
  private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
      RECIBIDO, Set.of(EN_PREPARACION, CANCELADO),
      EN_PREPARACION, Set.of(LISTO, CANCELADO),
      LISTO, Set.of(ENTREGADO),
      ENTREGADO, Set.of(),
      PAGADO, Set.of(),
      CANCELADO, Set.of());

  public boolean puedeCambiarA(EstadoPedido destino) {
    return TRANSICIONES.get(this).contains(destino);
  }

  public boolean esActivo() {
    return this != PAGADO && this != CANCELADO;
  }
}
