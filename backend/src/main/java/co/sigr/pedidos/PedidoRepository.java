package co.sigr.pedidos;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

  @EntityGraph(attributePaths = {"mesa", "mesero", "cliente", "detalles", "detalles.plato"})
  List<Pedido> findByEstadoInOrderByCreadoEnAsc(Collection<EstadoPedido> estados);

  @EntityGraph(attributePaths = {"mesa", "mesero", "cliente", "detalles", "detalles.plato"})
  Optional<Pedido> findWithDetallesById(Long id);

  @EntityGraph(attributePaths = {"mesa", "mesero", "cliente", "detalles", "detalles.plato"})
  List<Pedido> findByClienteEmailIgnoreCaseOrderByCreadoEnDesc(String email);

  boolean existsByMesaIdAndEstadoInAndClienteIdNot(Long mesaId, Collection<EstadoPedido> estados,
                                                   Long clienteId);

  boolean existsByMesaIdAndEstadoInAndClienteIsNull(Long mesaId,
                                                    Collection<EstadoPedido> estados);

  boolean existsByMesaIdAndEstadoIn(Long mesaId, Collection<EstadoPedido> estados);
}
