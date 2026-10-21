package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.service.CidadeService;
import br.com.inpetu.hub.validation.ContactRules;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

// Lote 4: cidades do mundo todo e telefone internacional.
class CidadesETelefoneTest {
  @Test
  void citySuggestionsComeFromTheGeocodingApiWithStateAndCountry() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    List<String> queries = Collections.synchronizedList(new ArrayList<>());
    HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    api.createContext(
        "/v1/search",
        exchange -> {
          calls.incrementAndGet();
          queries.add(exchange.getRequestURI().getRawQuery());
          byte[] body =
              ("{\"results\":[{\"name\":\"Lisboa\",\"admin1\":\"Lisboa\",\"country\":\"Portugal\"},"
                      + "{\"name\":\"Lisboa\",\"admin1\":\"Paraíba\",\"country\":\"Brasil\"},"
                      + "{\"name\":\"Lisboa\",\"admin1\":\"Lisboa\",\"country\":\"Portugal\"},"
                      + "{\"name\":\"Aeroporto de Lisboa\",\"feature_code\":\"AIRP\",\"country\":\"Portugal\"},"
                      + "{\"name\":\"Mônaco\",\"feature_code\":\"PPLC\",\"country\":\"Mônaco\"}]}")
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    api.start();
    try {
      var service =
          new CidadeService("http://127.0.0.1:" + api.getAddress().getPort() + "/v1/search");
      assertEquals(
          List.of("Lisboa, Portugal", "Lisboa, Paraíba, Brasil", "Mônaco"), service.buscar(" Lisb "));
      assertTrue(queries.get(0).contains("name=Lisb") && queries.get(0).contains("language=pt"));
      service.buscar("lisb");
      assertEquals(1, calls.get(), "termo repetido deveria vir do cache");
      assertEquals(List.of(), service.buscar("L"));
      assertEquals(List.of(), service.buscar("Lisboa'); DROP"));
      assertEquals(List.of(), service.buscar("x".repeat(61)));
      assertEquals(1, calls.get(), "termo inválido não pode ir para a API");
    } finally {
      api.stop(0);
    }
  }

  @Test
  void unavailableApiLeavesFreeTextWithoutSuggestions() {
    assertEquals(List.of(), new CidadeService("http://127.0.0.1:9/v1/search").buscar("Florianópolis"));
    assertEquals(List.of(), new CidadeService("").buscar("Florianópolis"));
  }

  @Test
  void phoneAcceptsBrazilAndInternationalNumbers() {
    for (String ok :
        List.of(
            "(48) 99808-2043",
            "4833334444",
            "+55 48 99999-0000",
            "+351 912 345 678",
            "+1 (415) 555-0100",
            "+376 312 345",
            "+44 20 7946 0958"))
      assertDoesNotThrow(() -> ContactRules.validate("Ana", "ana@exemplo.com", ok), ok);
    for (String bad :
        List.of("(48) 9980", "+351 12", "+1234567890123456", "48 9999 abcd 00", "351 912 345 678 90 12 3"))
      assertThrows(RegraException.class, () -> ContactRules.validate("Ana", "ana@exemplo.com", bad), bad);
  }
}
