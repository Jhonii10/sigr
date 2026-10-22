package co.sigr.menu;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatoRepository extends JpaRepository<Plato, Long> {

  @EntityGraph(attributePaths = "categoria")
  List<Plato> findAllByOrderByCategoriaIdAscNombreAsc();

  @EntityGraph(attributePaths = "categoria")
  List<Plato> findByDisponibleTrueOrderByCategoriaIdAscNombreAsc();

  boolean existsByCategoriaId(Long categoriaId);
}
