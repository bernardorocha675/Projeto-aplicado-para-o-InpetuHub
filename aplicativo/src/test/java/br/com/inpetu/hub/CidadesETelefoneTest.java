package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.service.CidadeService;
import br.com.inpetu.hub.validation.ContactRules;
import br.com.inpetu.hub.validation.Telefones;
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
              ("{\"results\":[{\"name\":\"Lisboa\",\"admin1\":\"Lisboa\",\"country\":\"Portugal\",\"country_code\":\"PT\"},"
                      + "{\"name\":\"Lisboa\",\"admin1\":\"Paraíba\",\"country\":\"Brasil\",\"country_code\":\"BR\"},"
                      + "{\"name\":\"Lisboa\",\"admin1\":\"Lisboa\",\"country\":\"Portugal\",\"country_code\":\"PT\"},"
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
          List.of(
              new CidadeService.Cidade("Lisboa, Portugal", "PT"),
              new CidadeService.Cidade("Lisboa, Paraíba, Brasil", "BR"),
              new CidadeService.Cidade("Mônaco", "")),
          service.buscar(" Lisb "));
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
  void phoneIsCheckedAndFormattedForTheChosenCountry() {
    assertEquals("(48) 99808-2043", Telefones.normalizar("BR", "48998082043"));
    assertEquals("(48) 3333-4444", Telefones.normalizar("BR", "(48) 3333-4444"));
    assertEquals("(48) 99999-0000", Telefones.normalizar("BR", "+55 48 99999-0000"));
    assertEquals("+351 912 345 678", Telefones.normalizar("PT", "912345678"));
    assertEquals("+351 912 345 678", Telefones.normalizar("BR", "+351912345678"), "o + vence o seletor");
    assertEquals("+1 650-253-0000", Telefones.normalizar("US", "6502530000"));
    assertEquals("+44 20 7946 0958", Telefones.normalizar("GB", "020 7946 0958"));
    for (String[] ruim :
        new String[][] {
          {"BR", "(48) 9980"}, {"BR", "4899808204399"}, {"PT", "12345"}, {"BR", "+351 12"},
          {"BR", "48 9999 abcd"}, {"BR", ""}, {"XX", "123"}
        })
      assertThrows(RegraException.class, () -> Telefones.normalizar(ruim[0], ruim[1]), ruim[1]);
    assertEquals("PT", Telefones.regiao("+351 912 345 678"));
    assertEquals("BR", Telefones.regiao("(48) 99808-2043"));
    assertEquals("BR", Telefones.PAISES.get(0).regiao());
    assertTrue(Telefones.PAISES.stream().anyMatch(p -> p.regiao().equals("PT") && p.nome().equals("Portugal") && p.codigo() == 351));
    assertTrue(Telefones.PAISES.size() > 200);
  }

  @Test
  void savedPhonesStayValidForContactRules() {
    for (String ok : List.of("(48) 99808-2043", "48999999999", "+55 (48) 99999-1234", "+351 912 345 678"))
      assertDoesNotThrow(() -> ContactRules.validate("Ana", "ana@exemplo.com", ok), ok);
    // Pedido gravado antes da 0.5.6 com o formato antigo continua aprovável.
    assertDoesNotThrow(() -> ContactRules.validate("Ana", "ana@exemplo.com", "00000000000"));
    assertThrows(RegraException.class, () -> ContactRules.validate("Ana", "ana@exemplo.com", "+1234567890123456"));
    assertThrows(RegraException.class, () -> ContactRules.validate("Ana", "ana@exemplo.com", "4899"));
  }
}
