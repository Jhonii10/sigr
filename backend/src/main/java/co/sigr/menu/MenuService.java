package co.sigr.menu;

import co.sigr.common.NegocioException;
import co.sigr.common.NoEncontradoException;
import co.sigr.menu.MenuDtos.CategoriaDto;
import co.sigr.menu.MenuDtos.CategoriaRequest;
import co.sigr.menu.MenuDtos.PlatoDto;
import co.sigr.menu.MenuDtos.PlatoRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

  private final CategoriaRepository categorias;
  private final PlatoRepository platos;

  public MenuService(CategoriaRepository categorias, PlatoRepository platos) {
    this.categorias = categorias;
    this.platos = platos;
  }

  @Transactional(readOnly = true)
  public List<CategoriaDto> listarCategorias() {
    return categorias.findAllByOrderByIdAsc().stream().map(CategoriaDto::de).toList();
  }

  @Transactional
  public CategoriaDto crearCategoria(CategoriaRequest req) {
    if (categorias.existsByNombreIgnoreCase(req.nombre().trim())) {
      throw new NegocioException("La categoría " + req.nombre() + " ya existe");
    }
    Categoria c = new Categoria();
    c.setNombre(req.nombre().trim());
    c.setDescripcion(req.descripcion());
    return CategoriaDto.de(categorias.save(c));
  }

  @Transactional
  public CategoriaDto actualizarCategoria(Long id, CategoriaRequest req) {
    Categoria c = categoria(id);
    c.setNombre(req.nombre().trim());
    c.setDescripcion(req.descripcion());
    return CategoriaDto.de(c);
  }

  @Transactional
  public void eliminarCategoria(Long id) {
    if (platos.existsByCategoriaId(id)) {
      throw new NegocioException("No se puede eliminar una categoría que tiene platos");
    }
    categorias.delete(categoria(id));
  }

  /** Menú público: solo platos disponibles. El administrador puede ver todos. */
  @Transactional(readOnly = true)
  public List<PlatoDto> listarPlatos(boolean incluirNoDisponibles) {
    List<Plato> lista = incluirNoDisponibles
        ? platos.findAllByOrderByCategoriaIdAscNombreAsc()
        : platos.findByDisponibleTrueOrderByCategoriaIdAscNombreAsc();
    return lista.stream().map(PlatoDto::de).toList();
  }

  @Transactional
  public PlatoDto crearPlato(PlatoRequest req) {
    Plato p = new Plato();
    aplicar(p, req);
    return PlatoDto.de(platos.save(p));
  }

  @Transactional
  public PlatoDto actualizarPlato(Long id, PlatoRequest req) {
    Plato p = plato(id);
    aplicar(p, req);
    return PlatoDto.de(p);
  }

  /** Los platos con pedidos asociados no se borran: se marcan como no disponibles. */
  @Transactional
  public void eliminarPlato(Long id) {
    plato(id).setDisponible(false);
  }

  @Transactional(readOnly = true)
  public Plato plato(Long id) {
    return platos.findById(id).orElseThrow(() -> new NoEncontradoException("Plato", id));
  }

  private Categoria categoria(Long id) {
    return categorias.findById(id).orElseThrow(() -> new NoEncontradoException("Categoría", id));
  }

  private void aplicar(Plato p, PlatoRequest req) {
    p.setCategoria(categoria(req.categoriaId()));
    p.setNombre(req.nombre().trim());
    p.setDescripcion(req.descripcion());
    p.setPrecio(req.precio());
    p.setDisponible(req.disponible() == null || req.disponible());
    p.setImagenUrl(req.imagenUrl());
  }
}
