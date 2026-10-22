package co.sigr.menu;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class MenuDtos {

  private MenuDtos() {
  }

  public record CategoriaRequest(
      @NotBlank @Size(max = 80) String nombre,
      @Size(max = 255) String descripcion) {
  }

  public record CategoriaDto(Long id, String nombre, String descripcion) {

    public static CategoriaDto de(Categoria c) {
      return new CategoriaDto(c.getId(), c.getNombre(), c.getDescripcion());
    }
  }

  public record PlatoRequest(
      @NotNull Long categoriaId,
      @NotBlank @Size(max = 120) String nombre,
      @Size(max = 500) String descripcion,
      @NotNull @DecimalMin(value = "0.0") BigDecimal precio,
      Boolean disponible,
      @Size(max = 500) String imagenUrl) {
  }

  public record PlatoDto(Long id, Long categoriaId, String categoria, String nombre,
                         String descripcion, BigDecimal precio, boolean disponible,
                         String imagenUrl) {

    public static PlatoDto de(Plato p) {
      return new PlatoDto(p.getId(), p.getCategoria().getId(), p.getCategoria().getNombre(),
          p.getNombre(), p.getDescripcion(), p.getPrecio(), p.isDisponible(), p.getImagenUrl());
    }
  }
}
