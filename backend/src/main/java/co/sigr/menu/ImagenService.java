package co.sigr.menu;

import co.sigr.common.NegocioException;
import co.sigr.common.NoEncontradoException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Guarda en disco las fotos de los platos y las sirve por la ruta pública del menú. */
@Service
public class ImagenService {

  public static final String RUTA_PUBLICA = "/api/menu/imagenes/";
  private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
  private static final Pattern NOMBRE_VALIDO =
      Pattern.compile("^plato-\\d+-[0-9a-f]{32}\\.(jpg|png|webp)$");

  /** Formatos aceptados, reconocidos por los primeros bytes del archivo y no por su nombre. */
  private enum Formato {
    JPG("jpg", MediaType.IMAGE_JPEG),
    PNG("png", MediaType.IMAGE_PNG),
    WEBP("webp", MediaType.parseMediaType("image/webp"));

    final String extension;
    final MediaType tipo;

    Formato(String extension, MediaType tipo) {
      this.extension = extension;
      this.tipo = tipo;
    }

    static Formato detectar(byte[] b) {
      if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8
          && (b[2] & 0xFF) == 0xFF) {
        return JPG;
      }
      if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
        return PNG;
      }
      if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
          && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
        return WEBP;
      }
      return null;
    }

    static Formato porExtension(String nombre) {
      return Arrays.stream(values()).filter(f -> nombre.endsWith("." + f.extension))
          .findFirst().orElseThrow();
    }
  }

  private final Path directorio;

  public ImagenService(@Value("${sigr.imagenes.directorio:data/imagenes}") String directorio) {
    this.directorio = Path.of(directorio).toAbsolutePath().normalize();
  }

  /** Guarda la imagen y devuelve la URL pública con la que se debe referenciar. */
  public String guardar(Long platoId, MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new NegocioException("Seleccione una imagen");
    }
    if (archivo.getSize() > TAMANO_MAXIMO) {
      throw new NegocioException("La imagen no puede superar 5 MB");
    }
    try {
      byte[] cabecera;
      try (InputStream in = archivo.getInputStream()) {
        cabecera = in.readNBytes(12);
      }
      Formato formato = Formato.detectar(cabecera);
      if (formato == null) {
        throw new NegocioException("Formato no permitido: use una imagen JPG, PNG o WEBP");
      }
      Files.createDirectories(directorio);
      String nombre = "plato-" + platoId + "-" + UUID.randomUUID().toString().replace("-", "")
          + "." + formato.extension;
      try (InputStream in = archivo.getInputStream()) {
        Files.copy(in, directorio.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
      }
      return RUTA_PUBLICA + nombre;
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo guardar la imagen", e);
    }
  }

  /** Borra el archivo si la URL apunta a una imagen guardada por este servicio. */
  public void eliminarSiEsLocal(String url) {
    if (url == null || !url.startsWith(RUTA_PUBLICA)) {
      return;
    }
    String nombre = url.substring(RUTA_PUBLICA.length());
    if (!NOMBRE_VALIDO.matcher(nombre).matches()) {
      return;
    }
    try {
      Files.deleteIfExists(directorio.resolve(nombre));
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo borrar la imagen anterior", e);
    }
  }

  public Resource leer(String nombre) {
    if (!NOMBRE_VALIDO.matcher(nombre).matches()) {
      throw new NoEncontradoException("Imagen", nombre);
    }
    Path ruta = directorio.resolve(nombre);
    if (!Files.isRegularFile(ruta)) {
      throw new NoEncontradoException("Imagen", nombre);
    }
    return new PathResource(ruta);
  }

  public MediaType tipo(String nombre) {
    return Formato.porExtension(nombre).tipo;
  }
}
