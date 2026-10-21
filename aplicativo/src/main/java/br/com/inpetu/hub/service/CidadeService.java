package br.com.inpetu.hub.service;

import br.com.inpetu.hub.observability.OperationalEvents;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sugestões de cidades do mundo todo pela API pública de geocodificação do Open-Meteo (sem chave).
 * A consulta sai do servidor local, pois a CSP impede o navegador de chamar sites externos. Sem
 * internet ou com a API fora do ar, não há sugestões e o campo continua aceitando texto livre.
 */
@Service
public class CidadeService {
  private static final String TERMO = "[\\p{L}\\p{M} .'’\\x2d]{2,60}";
  private static final int LIMITE = 10;
  private final HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(4))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();
  private final JsonMapper json = JsonMapper.builder().build();
  private final String api;
  private final Map<String, List<String>> cache =
      Collections.synchronizedMap(
          new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
              return size() > 200;
            }
          });

  public CidadeService(
      @Value("${app.cidades.api-url:https://geocoding-api.open-meteo.com/v1/search}") String api) {
    this.api = api;
  }

  public List<String> buscar(String termo) {
    if (termo == null || api.isBlank()) return List.of();
    String q = termo.strip();
    if (!q.matches(TERMO)) return List.of();
    String chave = q.toLowerCase(Locale.ROOT);
    List<String> salvo = cache.get(chave);
    if (salvo != null) return salvo;
    try {
      HttpRequest request =
          HttpRequest.newBuilder(
                  URI.create(
                      api
                          + "?count="
                          + LIMITE
                          + "&language=pt&format=json&name="
                          + URLEncoder.encode(q, StandardCharsets.UTF_8)))
              .timeout(Duration.ofSeconds(5))
              .GET()
              .build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200 || response.body().length() > 1_000_000)
        throw new IllegalStateException("resposta inesperada");
      Set<String> nomes = new LinkedHashSet<>();
      for (JsonNode item : json.readTree(response.body()).path("results")) {
        // Só localidades habitadas (PPL...): sem aeroportos, parques ou outros pontos com o mesmo nome.
        String tipo = item.path("feature_code").asString("");
        if (!tipo.isEmpty() && !tipo.startsWith("PPL")) continue;
        List<String> partes = new ArrayList<>();
        for (String campo : List.of("name", "admin1", "country")) {
          String valor = item.path(campo).asString("").strip();
          if (!valor.isEmpty() && !partes.contains(valor)) partes.add(valor);
        }
        if (!partes.isEmpty()) nomes.add(String.join(", ", partes));
        if (nomes.size() == LIMITE) break;
      }
      List<String> resultado = List.copyOf(nomes);
      cache.put(chave, resultado);
      return resultado;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return List.of();
    } catch (Exception e) {
      OperationalEvents.citySearchUnavailable(e);
      return List.of();
    }
  }
}
