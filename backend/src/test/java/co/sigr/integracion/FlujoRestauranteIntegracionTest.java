package co.sigr.integracion;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Pruebas de integración de los cinco módulos sobre H2 con las migraciones de Flyway. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlujoRestauranteIntegracionTest {

  @Autowired
  MockMvc mvc;
  @Autowired
  ObjectMapper json;

  private String login(String email, String password) throws Exception {
    String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return "Bearer " + json.readTree(body).get("token").asText();
  }

  private JsonNode leer(String body) throws Exception {
    return json.readTree(body);
  }

  @Test
  @Order(1)
  void menuPublicoSinAutenticacion() throws Exception {
    mvc.perform(get("/menu/platos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThan(0))));
  }

  @Test
  @Order(2)
  void rutasProtegidasExigenToken() throws Exception {
    mvc.perform(get("/pedidos")).andExpect(status().isUnauthorized());
    mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin@sigr.local\",\"password\":\"incorrecta\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(3)
  void adminGestionaMenu() throws Exception {
    String admin = login("admin@sigr.local", "Admin123*");
    mvc.perform(post("/menu/platos").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"categoriaId\":4,\"nombre\":\"Pandebono\",\"precio\":3500}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.categoria").value("Postres"));
  }

  @Test
  @Order(4)
  void clienteNoPuedeModificarMenu() throws Exception {
    mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Ana Cliente\",\"email\":\"ana@test.com\","
                + "\"password\":\"Cliente123\"}"))
        .andExpect(status().isCreated());
    String cliente = login("ana@test.com", "Cliente123");
    mvc.perform(post("/menu/platos").header("Authorization", cliente)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"categoriaId\":1,\"nombre\":\"X\",\"precio\":1}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @Order(5)
  void reservaYValidacionDeSolapamiento() throws Exception {
    String cliente = login("ana@test.com", "Cliente123");
    String fecha = LocalDate.now().plusDays(2).toString();
    String reserva = "{\"fecha\":\"" + fecha + "\",\"hora\":\"19:00\",\"personas\":8,\"mesaId\":8}";

    mvc.perform(post("/reservas").header("Authorization", cliente)
            .contentType(MediaType.APPLICATION_JSON).content(reserva))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.mesa").value(8));

    mvc.perform(post("/reservas").header("Authorization", cliente)
            .contentType(MediaType.APPLICATION_JSON)
            .content(reserva.replace("19:00", "20:00")))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/reservas/mias").header("Authorization", cliente))
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  @Order(6)
  void flujoCompletoPedidoPagoYCierreDeCaja() throws Exception {
    String admin = login("admin@sigr.local", "Admin123*");
    mvc.perform(post("/usuarios").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Luis Mesero\",\"email\":\"luis@test.com\","
                + "\"password\":\"Mesero123\",\"rol\":\"MESERO\"}"))
        .andExpect(status().isCreated());
    String mesero = login("luis@test.com", "Mesero123");

    String creado = mvc.perform(post("/pedidos").header("Authorization", mesero)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mesaId\":3,\"items\":[{\"platoId\":3,\"cantidad\":2},"
                + "{\"platoId\":6,\"cantidad\":2}]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("RECIBIDO"))
        .andExpect(jsonPath("$.total").value(72000.0))
        .andReturn().getResponse().getContentAsString();
    long pedidoId = leer(creado).get("id").asLong();

    mvc.perform(patch("/pedidos/" + pedidoId + "/estado").header("Authorization", mesero)
            .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"LISTO\"}"))
        .andExpect(status().isBadRequest());
    for (String estado : new String[] {"EN_PREPARACION", "LISTO", "ENTREGADO"}) {
      mvc.perform(patch("/pedidos/" + pedidoId + "/estado").header("Authorization", mesero)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"estado\":\"" + estado + "\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.estado").value(estado));
    }

    mvc.perform(post("/caja/abrir").header("Authorization", mesero)
            .contentType(MediaType.APPLICATION_JSON).content("{\"montoApertura\":100000}"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/caja/abrir").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"montoApertura\":100000}"))
        .andExpect(status().isCreated());
    mvc.perform(post("/caja/pagos").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"pedidoId\":" + pedidoId + ",\"medioPago\":\"EFECTIVO\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.valor").value(72000.0));
    mvc.perform(post("/caja/pagos").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"pedidoId\":" + pedidoId + ",\"medioPago\":\"EFECTIVO\"}"))
        .andExpect(status().isBadRequest());

    mvc.perform(post("/caja/cerrar").header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CERRADA"))
        .andExpect(jsonPath("$.totalVentas").value(72000.0))
        .andExpect(jsonPath("$.montoCierre").value(172000.0));

    mvc.perform(get("/caja/reporte").header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));

    mvc.perform(get("/mesas").header("Authorization", admin))
        .andExpect(jsonPath("$[2].estado").value("LIBRE"));
  }
}
