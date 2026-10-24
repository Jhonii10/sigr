package co.sigr.pedidos;

import co.sigr.pedidos.PedidoDtos.CambioEstadoRequest;
import co.sigr.pedidos.PedidoDtos.CrearPedidoRequest;
import co.sigr.pedidos.PedidoDtos.PedidoDto;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
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
@RequestMapping("/pedidos")
@PreAuthorize("hasAnyRole('MESERO','ADMIN')")
public class PedidoController {

  private final PedidoService service;

  public PedidoController(PedidoService service) {
    this.service = service;
  }

  @GetMapping
  public List<PedidoDto> activos() {
    return service.activos();
  }

  @GetMapping("/{id}")
  public PedidoDto obtener(@PathVariable Long id) {
    return service.obtener(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PedidoDto crear(@Valid @RequestBody CrearPedidoRequest req, Principal principal) {
    return service.crear(req, principal.getName());
  }

  @PatchMapping("/{id}/estado")
  public PedidoDto cambiarEstado(@PathVariable Long id,
                                 @Valid @RequestBody CambioEstadoRequest req) {
    return service.cambiarEstado(id, req.estado());
  }
}
