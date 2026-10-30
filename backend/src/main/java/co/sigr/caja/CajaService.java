package co.sigr.caja;

import co.sigr.auth.UsuarioService;
import co.sigr.caja.CajaDtos.PagoDto;
import co.sigr.caja.CajaDtos.ResumenCajaDto;
import co.sigr.common.NegocioException;
import co.sigr.pedidos.Pedido;
import co.sigr.pedidos.PedidoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CajaService {

  private final CajaRepository cajas;
  private final PagoRepository pagos;
  private final PedidoService pedidos;
  private final UsuarioService usuarios;

  public CajaService(CajaRepository cajas, PagoRepository pagos, PedidoService pedidos,
                     UsuarioService usuarios) {
    this.cajas = cajas;
    this.pagos = pagos;
    this.pedidos = pedidos;
    this.usuarios = usuarios;
  }

  @Transactional(readOnly = true)
  public Optional<ResumenCajaDto> actual() {
    return cajas.findFirstByEstado(EstadoCaja.ABIERTA).map(this::resumen);
  }

  @Transactional
  public ResumenCajaDto abrir(BigDecimal montoApertura, String email) {
    if (cajas.findFirstByEstado(EstadoCaja.ABIERTA).isPresent()) {
      throw new NegocioException("Ya hay una caja abierta; debe cerrarla antes de abrir otra");
    }
    Caja c = new Caja();
    c.setFecha(LocalDate.now());
    c.setMontoApertura(montoApertura);
    c.setUsuario(usuarios.porEmail(email));
    c.setAbiertaEn(LocalDateTime.now());
    return resumen(cajas.save(c));
  }

  @Transactional
  public PagoDto registrarPago(Long pedidoId, MedioPago medio) {
    Caja caja = cajaAbierta();
    if (pagos.existsByPedidoId(pedidoId)) {
      throw new NegocioException("El pedido " + pedidoId + " ya fue pagado");
    }
    Pedido pedido = pedidos.marcarPagado(pedidoId);
    Pago p = new Pago();
    p.setPedido(pedido);
    p.setCaja(caja);
    p.setMedioPago(medio);
    p.setValor(pedido.getTotal());
    return PagoDto.de(pagos.save(p));
  }

  /** Cierre: el monto de cierre es la apertura más el efectivo recibido. */
  @Transactional
  public ResumenCajaDto cerrar() {
    Caja c = cajaAbierta();
    ResumenCajaDto previo = resumen(c);
    c.setMontoCierre(previo.efectivoEsperado());
    c.setEstado(EstadoCaja.CERRADA);
    c.setCerradaEn(LocalDateTime.now());
    return resumen(c);
  }

  @Transactional(readOnly = true)
  public List<ResumenCajaDto> delDia(LocalDate fecha) {
    return cajas.findByFechaOrderByAbiertaEnAsc(fecha).stream().map(this::resumen).toList();
  }

  ResumenCajaDto resumen(Caja c) {
    List<PagoDto> lista = c.getId() == null ? List.of()
        : pagos.findByCajaIdInOrderByCreadoEnAsc(List.of(c.getId())).stream()
            .map(PagoDto::de).toList();
    Map<MedioPago, BigDecimal> porMedio = new EnumMap<>(MedioPago.class);
    for (MedioPago m : MedioPago.values()) {
      porMedio.put(m, BigDecimal.ZERO);
    }
    lista.forEach(p -> porMedio.merge(p.medioPago(), p.valor(), BigDecimal::add));
    BigDecimal total = porMedio.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal efectivoEsperado = c.getMontoApertura().add(porMedio.get(MedioPago.EFECTIVO));
    return new ResumenCajaDto(c.getId(), c.getFecha(), c.getEstado(), c.getMontoApertura(), total,
        porMedio, efectivoEsperado, c.getMontoCierre(), c.getAbiertaEn(), c.getCerradaEn(), lista);
  }

  private Caja cajaAbierta() {
    return cajas.findFirstByEstado(EstadoCaja.ABIERTA)
        .orElseThrow(() -> new NegocioException("No hay una caja abierta"));
  }
}
