package co.sigr.caja;

import co.sigr.caja.CajaDtos.AperturaRequest;
import co.sigr.caja.CajaDtos.PagoDto;
import co.sigr.caja.CajaDtos.PagoRequest;
import co.sigr.caja.CajaDtos.ResumenCajaDto;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/caja")
@PreAuthorize("hasRole('ADMIN')")
public class CajaController {

  private final CajaService service;
  private final ReporteVentasService reportes;

  public CajaController(CajaService service, ReporteVentasService reportes) {
    this.service = service;
    this.reportes = reportes;
  }

  @GetMapping("/actual")
  public ResponseEntity<ResumenCajaDto> actual() {
    return service.actual().map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PostMapping("/abrir")
  @ResponseStatus(HttpStatus.CREATED)
  public ResumenCajaDto abrir(@Valid @RequestBody AperturaRequest req, Principal principal) {
    return service.abrir(req.montoApertura(), principal.getName());
  }

  @PostMapping("/pagos")
  @ResponseStatus(HttpStatus.CREATED)
  public PagoDto pagar(@Valid @RequestBody PagoRequest req) {
    return service.registrarPago(req.pedidoId(), req.medioPago());
  }

  @PostMapping("/cerrar")
  public ResumenCajaDto cerrar() {
    return service.cerrar();
  }

  @GetMapping(value = "/reporte", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> reporte(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate fecha) {
    LocalDate dia = fecha == null ? LocalDate.now() : fecha;
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"reporte-ventas-" + dia + ".pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(reportes.generar(dia));
  }
}
