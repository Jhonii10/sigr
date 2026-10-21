package co.sigr.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crea el administrador inicial a partir de variables de entorno si no existe. */
@Component
public class AdminInicial implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(AdminInicial.class);

  private final UsuarioRepository repo;
  private final PasswordEncoder encoder;
  private final String email;
  private final String password;

  public AdminInicial(UsuarioRepository repo, PasswordEncoder encoder,
                      @Value("${sigr.admin.email}") String email,
                      @Value("${sigr.admin.password}") String password) {
    this.repo = repo;
    this.encoder = encoder;
    this.email = email;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!repo.existsByEmailIgnoreCase(email)) {
      repo.save(new Usuario("Administrador", email.toLowerCase(), encoder.encode(password), Rol.ADMIN));
      LOG.info("Usuario administrador inicial creado: {}", email);
    }
  }
}
