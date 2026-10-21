package co.sigr.auth;

import co.sigr.auth.AuthDtos.CrearUsuarioRequest;
import co.sigr.auth.AuthDtos.UsuarioDto;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

  private final UsuarioService service;

  public UsuarioController(UsuarioService service) {
    this.service = service;
  }

  @GetMapping
  public List<UsuarioDto> listar() {
    return service.listar();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioDto crear(@Valid @RequestBody CrearUsuarioRequest req) {
    return service.crearPorAdmin(req);
  }

  @PatchMapping("/{id}/activo")
  public UsuarioDto activo(@PathVariable Long id, @RequestBody Map<String, Boolean> body,
                           Principal principal) {
    return service.cambiarActivo(id, Boolean.TRUE.equals(body.get("activo")), principal.getName());
  }
}
