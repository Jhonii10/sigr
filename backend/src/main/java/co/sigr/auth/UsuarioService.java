package co.sigr.auth;

import co.sigr.auth.AuthDtos.CrearUsuarioRequest;
import co.sigr.auth.AuthDtos.LoginRequest;
import co.sigr.auth.AuthDtos.RegistroRequest;
import co.sigr.auth.AuthDtos.TokenResponse;
import co.sigr.auth.AuthDtos.UsuarioDto;
import co.sigr.common.JwtService;
import co.sigr.common.NegocioException;
import co.sigr.common.NoEncontradoException;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

  private final UsuarioRepository repo;
  private final PasswordEncoder encoder;
  private final AuthenticationManager authManager;
  private final JwtService jwtService;

  public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder,
                        AuthenticationManager authManager, JwtService jwtService) {
    this.repo = repo;
    this.encoder = encoder;
    this.authManager = authManager;
    this.jwtService = jwtService;
  }

  public TokenResponse login(LoginRequest req) {
    authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
    Usuario u = porEmail(req.email());
    return new TokenResponse(jwtService.generar(u.getEmail(), u.getRol().name()), UsuarioDto.de(u));
  }

  @Transactional
  public TokenResponse registrarCliente(RegistroRequest req) {
    Usuario u = crear(req.nombre(), req.email(), req.password(), Rol.CLIENTE);
    return new TokenResponse(jwtService.generar(u.getEmail(), u.getRol().name()), UsuarioDto.de(u));
  }

  @Transactional
  public UsuarioDto crearPorAdmin(CrearUsuarioRequest req) {
    return UsuarioDto.de(crear(req.nombre(), req.email(), req.password(), req.rol()));
  }

  @Transactional(readOnly = true)
  public List<UsuarioDto> listar() {
    return repo.findAllByOrderByNombreAsc().stream().map(UsuarioDto::de).toList();
  }

  @Transactional
  public UsuarioDto cambiarActivo(Long id, boolean activo, String emailSolicitante) {
    Usuario u = repo.findById(id).orElseThrow(() -> new NoEncontradoException("Usuario", id));
    if (u.getEmail().equalsIgnoreCase(emailSolicitante) && !activo) {
      throw new NegocioException("No puede desactivar su propio usuario");
    }
    u.setActivo(activo);
    return UsuarioDto.de(u);
  }

  @Transactional(readOnly = true)
  public Usuario porEmail(String email) {
    return repo.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new NegocioException("Usuario no encontrado: " + email));
  }

  private Usuario crear(String nombre, String email, String password, Rol rol) {
    if (repo.existsByEmailIgnoreCase(email)) {
      throw new NegocioException("Ya existe un usuario con el email " + email);
    }
    return repo.save(new Usuario(nombre.trim(), email.trim().toLowerCase(),
        encoder.encode(password), rol));
  }
}
