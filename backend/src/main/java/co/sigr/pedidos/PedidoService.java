package co.sigr.pedidos;

import co.sigr.auth.Rol;
import co.sigr.auth.Usuario;
import co.sigr.auth.UsuarioService;
import co.sigr.common.NegocioException;
import co.sigr.common.NoEncontradoException;
import co.sigr.menu.MenuService;
import co.sigr.menu.Plato;
import co.sigr.pedidos.PedidoDtos.CrearPedidoRequest;
import co.sigr.pedidos.PedidoDtos.ItemRequest;
import co.sigr.pedidos.PedidoDtos.PedidoDto;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

  public static final String TOPICO = "/topic/pedidos";
  private static final Set<EstadoPedido> ACTIVOS = EnumSet.of(
      EstadoPedido.RECIBIDO, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO,
      EstadoPedido.ENTREGADO);

  private final PedidoRepository pedidos;
  private final MesaRepository mesas;
  private final MenuService menu;
  private final UsuarioService usuarios;
  private final SimpMessagingTemplate mensajeria;

  public PedidoService(PedidoRepository pedidos, MesaRepository mesas, MenuService menu,
                       UsuarioService usuarios, SimpMessagingTemplate mensajeria) {
    this.pedidos = pedidos;
    this.mesas = mesas;
    this.menu = menu;
    this.usuarios = usuarios;
    this.mensajeria = mensajeria;
  }

  @Transactional(readOnly = true)
  public List<PedidoDto> activos() {
    return pedidos.findByEstadoInOrderByCreadoEnAsc(ACTIVOS).stream().map(PedidoDto::de).toList();
  }

  @Transactional(readOnly = true)
  public PedidoDto obtener(Long id) {
    return PedidoDto.de(pedido(id));
  }

  @Transactional(readOnly = true)
  public List<PedidoDto> delCliente(String emailCliente) {
    return pedidos.findByClienteEmailIgnoreCaseOrderByCreadoEnDesc(emailCliente).stream()
        .map(PedidoDto::de).toList();
  }

  /**
   * Registra un pedido. Si lo hace el personal queda como mesero; si lo hace un cliente desde
   * el menú queda como cliente, y solo puede pedir en una mesa libre o en la que ya tiene pedidos.
   */
  @Transactional
  public PedidoDto crear(CrearPedidoRequest req, String emailSolicitante) {
    Mesa mesa = mesas.findById(req.mesaId())
        .orElseThrow(() -> new NoEncontradoException("Mesa", req.mesaId()));
    Usuario solicitante = usuarios.porEmail(emailSolicitante);
    Pedido p = new Pedido();
    p.setMesa(mesa);
    if (solicitante.getRol() == Rol.CLIENTE) {
      validarMesaParaCliente(mesa, solicitante);
      p.setCliente(solicitante);
    } else {
      p.setMesero(solicitante);
    }
    p.setNota(req.nota());
    for (ItemRequest item : req.items()) {
      Plato plato = menu.plato(item.platoId());
      if (!plato.isDisponible()) {
        throw new NegocioException("El plato " + plato.getNombre() + " no está disponible");
      }
      p.agregarDetalle(new DetallePedido(plato, item.cantidad()));
    }
    p.recalcularTotal();
    mesa.setEstado(EstadoMesa.OCUPADA);
    PedidoDto dto = PedidoDto.de(pedidos.save(p));
    publicar(dto);
    return dto;
  }

  @Transactional
  public PedidoDto cambiarEstado(Long id, EstadoPedido destino, String emailPersonal) {
    Pedido p = pedido(id);
    if (!p.getEstado().puedeCambiarA(destino)) {
      throw new NegocioException("No se puede pasar un pedido de " + p.getEstado()
          + " a " + destino);
    }
    p.setEstado(destino);
    if (p.getMesero() == null) {
      p.setMesero(usuarios.porEmail(emailPersonal));
    }
    if (destino == EstadoPedido.CANCELADO) {
      liberarMesaSiCorresponde(p);
    }
    PedidoDto dto = PedidoDto.de(p);
    publicar(dto);
    return dto;
  }

  /** Usado por el módulo de caja al registrar el pago. */
  @Transactional
  public Pedido marcarPagado(Long id) {
    Pedido p = pedido(id);
    if (p.getEstado() != EstadoPedido.ENTREGADO) {
      throw new NegocioException("Solo se pueden pagar pedidos en estado ENTREGADO");
    }
    p.setEstado(EstadoPedido.PAGADO);
    liberarMesaSiCorresponde(p);
    publicar(PedidoDto.de(p));
    return p;
  }

  private void validarMesaParaCliente(Mesa mesa, Usuario cliente) {
    boolean atendidaPorOtro =
        pedidos.existsByMesaIdAndEstadoInAndClienteIsNull(mesa.getId(), ACTIVOS)
        || pedidos.existsByMesaIdAndEstadoInAndClienteIdNot(mesa.getId(), ACTIVOS,
            cliente.getId());
    if (atendidaPorOtro) {
      throw new NegocioException("La mesa " + mesa.getNumero()
          + " ya tiene pedidos de otras personas. Pídale el pedido al mesero.");
    }
  }

  private void liberarMesaSiCorresponde(Pedido p) {
    pedidos.flush();
    if (!pedidos.existsByMesaIdAndEstadoIn(p.getMesa().getId(), ACTIVOS)) {
      p.getMesa().setEstado(EstadoMesa.LIBRE);
    }
  }

  private Pedido pedido(Long id) {
    return pedidos.findWithDetallesById(id)
        .orElseThrow(() -> new NoEncontradoException("Pedido", id));
  }

  private void publicar(PedidoDto dto) {
    mensajeria.convertAndSend(TOPICO, dto);
  }
}
