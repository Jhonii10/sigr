package co.sigr.caja;

import co.sigr.caja.CajaDtos.PagoDto;
import co.sigr.caja.CajaDtos.ResumenCajaDto;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Genera el reporte de ventas diario en PDF. */
@Service
public class ReporteVentasService {

  private static final Color NARANJA = new Color(232, 119, 34);
  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

  private final CajaService cajaService;

  public ReporteVentasService(CajaService cajaService) {
    this.cajaService = cajaService;
  }

  public byte[] generar(LocalDate fecha) {
    List<ResumenCajaDto> cajas = cajaService.delDia(fecha);
    NumberFormat cop = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    cop.setMaximumFractionDigits(0);

    Map<MedioPago, BigDecimal> totales = new EnumMap<>(MedioPago.class);
    for (MedioPago m : MedioPago.values()) {
      totales.put(m, BigDecimal.ZERO);
    }
    cajas.forEach(c -> c.totalesPorMedio().forEach((m, v) -> totales.merge(m, v, BigDecimal::add)));
    BigDecimal granTotal = totales.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document doc = new Document(PageSize.LETTER, 40, 40, 40, 40);
    PdfWriter.getInstance(doc, out);
    doc.open();

    Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, NARANJA);
    Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);

    doc.add(new Paragraph("SIGR - Reporte de ventas diario", titulo));
    doc.add(new Paragraph("Fecha: " + fecha.format(FECHA) + "    Cajas: " + cajas.size(), normal));
    doc.add(new Paragraph(" "));

    PdfPTable resumen = new PdfPTable(2);
    resumen.setWidthPercentage(60);
    resumen.setHorizontalAlignment(Element.ALIGN_LEFT);
    encabezado(resumen, "Medio de pago", "Total");
    totales.forEach((m, v) -> fila(resumen, normal, m.name(), cop.format(v)));
    fila(resumen, negrita, "TOTAL VENTAS", cop.format(granTotal));
    doc.add(resumen);
    doc.add(new Paragraph(" "));

    for (ResumenCajaDto c : cajas) {
      doc.add(new Paragraph("Caja #" + c.id() + " - " + c.estado()
          + " | Apertura: " + cop.format(c.montoApertura())
          + " | Cierre: " + (c.montoCierre() == null ? "pendiente" : cop.format(c.montoCierre())),
          negrita));
      PdfPTable detalle = new PdfPTable(new float[] {1.2f, 1, 1, 2, 2});
      detalle.setWidthPercentage(100);
      detalle.setSpacingBefore(4);
      detalle.setSpacingAfter(12);
      encabezado(detalle, "Hora", "Pedido", "Mesa", "Medio", "Valor");
      for (PagoDto p : c.pagos()) {
        fila(detalle, normal, p.creadoEn().format(HORA), "#" + p.pedidoId(),
            String.valueOf(p.mesa()), p.medioPago().name(), cop.format(p.valor()));
      }
      if (c.pagos().isEmpty()) {
        PdfPCell vacio = new PdfPCell(new Phrase("Sin pagos registrados", normal));
        vacio.setColspan(5);
        detalle.addCell(vacio);
      }
      doc.add(detalle);
    }
    doc.close();
    return out.toByteArray();
  }

  private static void encabezado(PdfPTable t, String... textos) {
    Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    for (String s : textos) {
      PdfPCell c = new PdfPCell(new Phrase(s, f));
      c.setBackgroundColor(new Color(30, 58, 95));
      c.setPadding(5);
      t.addCell(c);
    }
  }

  private static void fila(PdfPTable t, Font f, String... textos) {
    for (String s : textos) {
      PdfPCell c = new PdfPCell(new Phrase(s, f));
      c.setPadding(4);
      t.addCell(c);
    }
  }
}
