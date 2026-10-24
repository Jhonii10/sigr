package co.sigr.pedidos;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mesas")
public class MesaController {

  public record MesaDto(Long id, Integer numero, Integer capacidad, EstadoMesa estado) {

    public static MesaDto de(Mesa m) {
      return new MesaDto(m.getId(), m.getNumero(), m.getCapacidad(), m.getEstado());
    }
  }

  private final MesaRepository repo;

  public MesaController(MesaRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<MesaDto> listar() {
    return repo.findAllByOrderByNumeroAsc().stream().map(MesaDto::de).toList();
  }
}
