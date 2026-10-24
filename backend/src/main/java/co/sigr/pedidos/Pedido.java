package co.sigr.pedidos;

import co.sigr.auth.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mesa_id")
  private Mesa mesa;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mesero_id")
  private Usuario mesero;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoPedido estado = EstadoPedido.RECIBIDO;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total = BigDecimal.ZERO;

  @Column(length = 255)
  private String nota;

  @Column(name = "creado_en", nullable = false)
  private LocalDateTime creadoEn = LocalDateTime.now();

  @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DetallePedido> detalles = new ArrayList<>();

  public void agregarDetalle(DetallePedido d) {
    d.setPedido(this);
    detalles.add(d);
  }

  public void recalcularTotal() {
    total = detalles.stream()
        .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
