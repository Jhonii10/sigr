package co.sigr.caja;

import co.sigr.auth.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "caja")
@Getter
@Setter
@NoArgsConstructor
public class Caja {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private LocalDate fecha;

  @Column(name = "monto_apertura", nullable = false, precision = 12, scale = 2)
  private BigDecimal montoApertura;

  @Column(name = "monto_cierre", precision = 12, scale = 2)
  private BigDecimal montoCierre;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoCaja estado = EstadoCaja.ABIERTA;

  @Column(name = "abierta_en", nullable = false)
  private LocalDateTime abiertaEn;

  @Column(name = "cerrada_en")
  private LocalDateTime cerradaEn;
}
