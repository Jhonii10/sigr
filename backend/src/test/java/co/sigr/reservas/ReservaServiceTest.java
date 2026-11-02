package co.sigr.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import co.sigr.auth.UsuarioService;
import co.sigr.common.NegocioException;
import co.sigr.pedidos.Mesa;
import co.sigr.pedidos.MesaRepository;
import co.sigr.reservas.ReservaDtos.CrearReservaRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

  @Mock
  ReservaRepository reservas;
  @Mock
  MesaRepository mesas;
  @Mock
  UsuarioService usuarios;

  ReservaService service;
  Mesa mesa;
  LocalDate manana = LocalDate.now().plusDays(1);

  @BeforeEach
  void setUp() {
    service = new ReservaService(reservas, mesas, usuarios, 120);
    mesa = new Mesa();
    mesa.setId(3L);
    mesa.setNumero(3);
    mesa.setCapacidad(4);
  }

  private Reserva reservaA(LocalTime hora) {
    Reserva r = new Reserva();
    r.setMesa(mesa);
    r.setFecha(manana);
    r.setHora(hora);
    return r;
  }

  @Test
  void detectaSolapamientoDentroDeLaFranja() {
    when(reservas.findByMesaIdAndFechaAndEstado(3L, manana, EstadoReserva.CONFIRMADA))
        .thenReturn(List.of(reservaA(LocalTime.of(13, 0))));

    assertThat(service.haySolapamiento(mesa, manana, LocalTime.of(14, 30))).isTrue();
    assertThat(service.haySolapamiento(mesa, manana, LocalTime.of(11, 30))).isTrue();
  }

  @Test
  void permiteReservaFueraDeLaFranja() {
    when(reservas.findByMesaIdAndFechaAndEstado(3L, manana, EstadoReserva.CONFIRMADA))
        .thenReturn(List.of(reservaA(LocalTime.of(13, 0))));

    assertThat(service.haySolapamiento(mesa, manana, LocalTime.of(15, 0))).isFalse();
  }

  @Test
  void rechazaReservaSinMesasDisponibles() {
    when(mesas.findByCapacidadGreaterThanEqualOrderByCapacidadAscNumeroAsc(anyInt()))
        .thenReturn(List.of(mesa));
    when(reservas.findByMesaIdAndFechaAndEstado(eq(3L), any(), any()))
        .thenReturn(List.of(reservaA(LocalTime.of(19, 0))));

    var req = new CrearReservaRequest(manana, LocalTime.of(19, 30), 4, null);

    assertThatThrownBy(() -> service.crear(req, "cliente@test.com"))
        .isInstanceOf(NegocioException.class)
        .hasMessageContaining("No hay mesas disponibles");
  }

  @Test
  void rechazaHorarioFueraDeServicio() {
    assertThatThrownBy(() -> service.disponibilidad(manana, LocalTime.of(23, 0), 2))
        .isInstanceOf(NegocioException.class)
        .hasMessageContaining("11:00");
  }
}
