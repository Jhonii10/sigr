package co.sigr.caja;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CajaRepository extends JpaRepository<Caja, Long> {

  Optional<Caja> findFirstByEstado(EstadoCaja estado);

  List<Caja> findByFechaOrderByAbiertaEnAsc(LocalDate fecha);
}
