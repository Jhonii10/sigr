package co.sigr.auth;

import co.sigr.auth.AuthDtos.LoginRequest;
import co.sigr.auth.AuthDtos.RegistroRequest;
import co.sigr.auth.AuthDtos.TokenResponse;
import co.sigr.auth.AuthDtos.UsuarioDto;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UsuarioService service;

  public AuthController(UsuarioService service) {
    this.service = service;
  }

  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest req) {
    return service.login(req);
  }

  @PostMapping("/registro")
  @ResponseStatus(HttpStatus.CREATED)
  public TokenResponse registro(@Valid @RequestBody RegistroRequest req) {
    return service.registrarCliente(req);
  }

  @GetMapping("/me")
  public UsuarioDto me(Principal principal) {
    return UsuarioDto.de(service.porEmail(principal.getName()));
  }
}
