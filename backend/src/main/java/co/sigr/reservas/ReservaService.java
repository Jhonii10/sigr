package co.sigr.reservas;

import co.sigr.auth.Rol;
import co.sigr.auth.Usuario;
import co.sigr.auth.UsuarioService;
import co.sigr.common.NegocioException;
import co.sigr.common.NoEncontradoException;
import co.sigr.pedidos.Mesa;
import co.sigr.pedidos.MesaController.MesaDto;
import co.sigr.pedidos.MesaRepository;
import co.sigr.reservas.ReservaDtos.CrearReservaRequest;
import co.sigr.reservas.ReservaDtos.ReservaDto;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaService {

  static final LocalTime APERTURA = LocalTime.of(11, 0);
  static final LocalTime ULTIMA_RESERVA = LocalTime.of(21, 0);

  private final ReservaRepository reservas;
  private final MesaRepository mesas;
  private final UsuarioService usuarios;
  private final long duracionMinutos;

  public ReservaService(ReservaRepository reservas, MesaRepository mesas, UsuarioService usuarios,
                        @Value("${sigr.reservas.duracion-minutos:120}") long duracionMinutos) {
    this.reservas = reservas;
    this.mesas = mesas;
    this.usuarios = usuarios;
    this.duracionMinutos = duracionMinutos;
  }

  /** Mesas con capacidad suficiente y sin reservas que se solapen con la franja pedida. */
  @Transactional(readOnly = true)
  public List<MesaDto> disponibilidad(LocalDate fecha, LocalTime hora, int personas) {
    validarHorario(fecha, hora);
    return mesasLibres(fecha, hora, personas).stream().map(MesaDto::de).toList();
  }

  @Transactional
  public ReservaDto crear(CrearReservaRequest req, String emailCliente) {
    validarHorario(req.fecha(), req.hora());
    List<Mesa> libres = mesasLibres(req.fecha(), req.hora(), req.personas());
    Mesa mesa;
    if (req.mesaId() != null) {
      mesa = libres.stream().filter(m -> m.getId().equals(req.mesaId())).findFirst()
          .orElseThrow(() -> new NegocioException(
              "La mesa seleccionada no está disponible para esa fecha, hora y número de personas"));
    } else {
      mesa = libres.stream().findFirst()
          .orElseThrow(() -> new NegocioException("No hay mesas disponibles para esa franja"));
    }
    Reserva r = new Reserva();
    r.setCliente(usuarios.porEmail(emailCliente));
    r.setMesa(mesa);
    r.setFecha(req.fecha());
    r.setHora(req.hora());
    r.setPersonas(req.personas());
    return ReservaDto.de(reservas.save(r));
  }

  @Transactional(readOnly = true)
  public List<ReservaDto> mias(String email) {
    return reservas.findByClienteEmailIgnoreCaseOrderByFechaDescHoraDesc(email).stream()
        .map(ReservaDto::de).toList();
  }

  @Transactional(readOnly = true)
  public List<ReservaDto> delDia(LocalDate fecha) {
    return reservas.findByFechaOrderByHoraAsc(fecha).stream().map(ReservaDto::de).toList();
  }

  @Transactional
  public ReservaDto cancelar(Long id, String emailSolicitante) {
    Reserva r = reservas.findById(id).orElseThrow(() -> new NoEncontradoException("Reserva", id));
    Usuario solicitante = usuarios.porEmail(emailSolicitante);
    boolean esDueno = r.getCliente().getId().equals(solicitante.getId());
    if (!esDueno && solicitante.getRol() == Rol.CLIENTE) {
      throw new NegocioException("Solo puede cancelar sus propias reservas");
    }
    if (r.getEstado() == EstadoReserva.CANCELADA) {
      throw new NegocioException("La reserva ya está cancelada");
    }
    r.setEstado(EstadoReserva.CANCELADA);
    return ReservaDto.de(r);
  }

  List<Mesa> mesasLibres(LocalDate fecha, LocalTime hora, int personas) {
    return mesas.findByCapacidadGreaterThanEqualOrderByCapacidadAscNumeroAsc(personas).stream()
        .filter(m -> !haySolapamiento(m, fecha, hora))
        .toList();
  }

  boolean haySolapamiento(Mesa mesa, LocalDate fecha, LocalTime hora) {
    return reservas.findByMesaIdAndFechaAndEstado(mesa.getId(), fecha, EstadoReserva.CONFIRMADA)
        .stream()
        .anyMatch(r -> Math.abs(Duration.between(r.getHora(), hora).toMinutes()) < duracionMinutos);
  }

  private void validarHorario(LocalDate fecha, LocalTime hora) {
    if (hora.isBefore(APERTURA) || hora.isAfter(ULTIMA_RESERVA)) {
      throw new NegocioException("Las reservas se reciben entre las 11:00 y las 21:00");
    }
    if (LocalDateTime.of(fecha, hora).isBefore(LocalDateTime.now())) {
      throw new NegocioException("No se puede reservar en una fecha u hora pasada");
    }
  }
}
