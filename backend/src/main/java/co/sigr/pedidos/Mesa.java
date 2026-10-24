package co.sigr.pedidos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mesa")
@Getter
@Setter
@NoArgsConstructor
public class Mesa {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Integer numero;

  @Column(nullable = false)
  private Integer capacidad;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoMesa estado = EstadoMesa.LIBRE;
}
