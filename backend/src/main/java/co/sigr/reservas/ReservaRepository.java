package co.sigr.reservas;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

  List<Reserva> findByMesaIdAndFechaAndEstado(Long mesaId, LocalDate fecha, EstadoReserva estado);

  @EntityGraph(attributePaths = {"mesa", "cliente"})
  List<Reserva> findByClienteEmailIgnoreCaseOrderByFechaDescHoraDesc(String email);

  @EntityGraph(attributePaths = {"mesa", "cliente"})
  List<Reserva> findByFechaOrderByHoraAsc(LocalDate fecha);
}
