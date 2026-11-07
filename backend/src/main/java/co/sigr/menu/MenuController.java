package co.sigr.menu;

import co.sigr.menu.MenuDtos.CategoriaDto;
import co.sigr.menu.MenuDtos.CategoriaRequest;
import co.sigr.menu.MenuDtos.PlatoDto;
import co.sigr.menu.MenuDtos.PlatoRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/menu")
public class MenuController {

  private final MenuService service;
  private final ImagenService imagenes;

  public MenuController(MenuService service, ImagenService imagenes) {
    this.service = service;
    this.imagenes = imagenes;
  }

  @GetMapping("/categorias")
  public List<CategoriaDto> categorias() {
    return service.listarCategorias();
  }

  @GetMapping("/platos")
  public List<PlatoDto> platos(@RequestParam(defaultValue = "false") boolean todos,
                               Authentication auth) {
    boolean esAdmin = auth != null && auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    return service.listarPlatos(todos && esAdmin);
  }

  @PostMapping("/categorias")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public CategoriaDto crearCategoria(@Valid @RequestBody CategoriaRequest req) {
    return service.crearCategoria(req);
  }

  @PutMapping("/categorias/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public CategoriaDto actualizarCategoria(@PathVariable Long id,
                                          @Valid @RequestBody CategoriaRequest req) {
    return service.actualizarCategoria(id, req);
  }

  @DeleteMapping("/categorias/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarCategoria(@PathVariable Long id) {
    service.eliminarCategoria(id);
  }

  @PostMapping("/platos")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public PlatoDto crearPlato(@Valid @RequestBody PlatoRequest req) {
    return service.crearPlato(req);
  }

  @PutMapping("/platos/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public PlatoDto actualizarPlato(@PathVariable Long id, @Valid @RequestBody PlatoRequest req) {
    return service.actualizarPlato(id, req);
  }

  @DeleteMapping("/platos/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarPlato(@PathVariable Long id) {
    service.eliminarPlato(id);
  }

  @PostMapping(path = "/platos/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public PlatoDto subirImagen(@PathVariable Long id,
                              @RequestParam("archivo") MultipartFile archivo) {
    return service.subirImagen(id, archivo);
  }

  @DeleteMapping("/platos/{id}/imagen")
  @PreAuthorize("hasRole('ADMIN')")
  public PlatoDto quitarImagen(@PathVariable Long id) {
    return service.quitarImagen(id);
  }

  /** Foto pública de un plato. Los nombres llevan un identificador único, así que se cachean. */
  @GetMapping("/imagenes/{nombre:.+}")
  public ResponseEntity<Resource> imagen(@PathVariable String nombre) {
    Resource archivo = imagenes.leer(nombre);
    return ResponseEntity.ok()
        .contentType(imagenes.tipo(nombre))
        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
        .body(archivo);
  }
}
