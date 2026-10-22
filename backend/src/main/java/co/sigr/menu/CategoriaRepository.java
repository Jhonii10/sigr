package co.sigr.menu;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

  List<Categoria> findAllByOrderByIdAsc();

  boolean existsByNombreIgnoreCase(String nombre);
}
