package co.sigr.pedidos;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

  @EntityGraph(attributePaths = {"mesa", "mesero", "detalles", "detalles.plato"})
  List<Pedido> findByEstadoInOrderByCreadoEnAsc(Collection<EstadoPedido> estados);

  @EntityGraph(attributePaths = {"mesa", "mesero", "detalles", "detalles.plato"})
  Optional<Pedido> findWithDetallesById(Long id);

  boolean existsByMesaIdAndEstadoIn(Long mesaId, Collection<EstadoPedido> estados);
}
