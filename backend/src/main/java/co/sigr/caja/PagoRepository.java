package co.sigr.caja;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

  @EntityGraph(attributePaths = {"pedido", "pedido.mesa"})
  List<Pago> findByCajaIdInOrderByCreadoEnAsc(Collection<Long> cajaIds);

  boolean existsByPedidoId(Long pedidoId);
}
