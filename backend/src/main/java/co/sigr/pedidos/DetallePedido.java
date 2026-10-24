package co.sigr.pedidos;

import co.sigr.menu.Plato;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalle_pedido")
@Getter
@Setter
@NoArgsConstructor
public class DetallePedido {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pedido_id")
  private Pedido pedido;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plato_id")
  private Plato plato;

  @Column(nullable = false)
  private Integer cantidad;

  /** Precio congelado al momento del pedido. */
  @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
  private BigDecimal precioUnitario;

  public DetallePedido(Plato plato, Integer cantidad) {
    this.plato = plato;
    this.cantidad = cantidad;
    this.precioUnitario = plato.getPrecio();
  }
}
