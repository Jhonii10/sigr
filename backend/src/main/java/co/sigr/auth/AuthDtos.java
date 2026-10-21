package co.sigr.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

  private AuthDtos() {
  }

  public record LoginRequest(
      @NotBlank @Email String email,
      @NotBlank String password) {
  }

  public record RegistroRequest(
      @NotBlank @Size(max = 120) String nombre,
      @NotBlank @Email @Size(max = 150) String email,
      @NotBlank @Size(min = 8, max = 64, message = "debe tener entre 8 y 64 caracteres")
      String password) {
  }

  public record CrearUsuarioRequest(
      @NotBlank @Size(max = 120) String nombre,
      @NotBlank @Email @Size(max = 150) String email,
      @NotBlank @Size(min = 8, max = 64, message = "debe tener entre 8 y 64 caracteres")
      String password,
      @NotNull Rol rol) {
  }

  public record UsuarioDto(Long id, String nombre, String email, Rol rol, boolean activo) {

    public static UsuarioDto de(Usuario u) {
      return new UsuarioDto(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.isActivo());
    }
  }

  public record TokenResponse(String token, UsuarioDto usuario) {
  }
}
