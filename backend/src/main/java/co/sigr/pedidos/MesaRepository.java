package co.sigr.pedidos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

  List<Mesa> findAllByOrderByNumeroAsc();

  List<Mesa> findByCapacidadGreaterThanEqualOrderByCapacidadAscNumeroAsc(Integer personas);
}
