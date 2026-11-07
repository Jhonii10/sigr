package co.sigr.reservas;

import co.sigr.pedidos.MesaController.MesaDto;
import co.sigr.reservas.ReservaDtos.CrearReservaRequest;
import co.sigr.reservas.ReservaDtos.ReservaDto;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

  private final ReservaService service;

  public ReservaController(ReservaService service) {
    this.service = service;
  }

  @GetMapping("/disponibilidad")
  public List<MesaDto> disponibilidad(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
      @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime hora,
      @RequestParam int personas) {
    return service.disponibilidad(fecha, hora, personas);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReservaDto crear(@Valid @RequestBody CrearReservaRequest req, Principal principal) {
    return service.crear(req, principal.getName());
  }

  @GetMapping("/mias")
  public List<ReservaDto> mias(Principal principal) {
    return service.mias(principal.getName());
  }

  /** Con {@code fecha} devuelve las reservas de ese día; con {@code desde}, las de ahí en adelante. */
  @GetMapping
  @PreAuthorize("hasAnyRole('MESERO','ADMIN')")
  public List<ReservaDto> listar(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate fecha,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate desde) {
    if (fecha != null) {
      return service.delDia(fecha);
    }
    return service.desde(desde != null ? desde : LocalDate.now());
  }

  @PatchMapping("/{id}/cancelar")
  public ReservaDto cancelar(@PathVariable Long id, Principal principal) {
    return service.cancelar(id, principal.getName());
  }
}
