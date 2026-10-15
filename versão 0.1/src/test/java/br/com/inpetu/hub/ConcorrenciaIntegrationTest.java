package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import br.com.inpetu.hub.dto.*;
import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.exception.*;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.service.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
      "spring.config.import=",
      "spring.datasource.url=${TEST_CONCURRENT_DB_URL:jdbc:h2:mem:concorrencia;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}",
      "app.admin-login=admin@inpetuhub.local",
      "app.admin-password=Inpetu@123"
    })
@ActiveProfiles("test")
@Import(FluxosIntegrationTest.Tempo.class)
class ConcorrenciaIntegrationTest {
  @Autowired ReservaService service;
  @Autowired EspacoRepository espacos;
  @Autowired PersonaRepository personas;

  ReservaForm form(String codigo, Long espaco) {
    var f = new ReservaForm();
    f.espacoId = espaco;
    f.personaId = personas.findByCodigo(codigo).orElseThrow().id;
    f.data = LocalDate.of(2026, 11, 10);
    f.horaInicio = LocalTime.of(8, 0);
    f.horaFim = LocalTime.of(12, 0);
    f.numeroParticipantes = 1;
    f.cnpj = "98765432000100";
    f.nome = "Concorrência";
    f.email = "concorrencia@example.com";
    f.telefone = "48999999999";
    f.cidade = "Florianópolis";
    f.empresa = "Teste";
    f.titulo = "Teste concorrente";
    f.objetivo = "Validar atomicidade";
    return f;
  }

  Long sala() {
    Espaco e = new Espaco();
    e.nomeEspaco = "Sala concorrência";
    e.capacidade = 20;
    e.ativo = true;
    e.disponibilidade = true;
    return espacos.saveAndFlush(e).id;
  }

  List<String> simultaneas(ReservaForm a, ReservaForm b) throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    var inicio = new CountDownLatch(1);
    try {
      List<Future<String>> futuros = new ArrayList<>();
      for (ReservaForm f : List.of(a, b))
        futuros.add(
            pool.submit(
                () -> {
                  inicio.await();
                  try {
                    var r = service.solicitar(f);
                    return r.usaCota ? "COTA" : "SEM_COTA";
                  } catch (RegraException ex) {
                    return "CONFLITO";
                  }
                }));
      inicio.countDown();
      List<String> resultados = new ArrayList<>();
      for (var futuro : futuros) resultados.add(futuro.get(15, TimeUnit.SECONDS));
      return resultados;
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void somenteUmPedidoOcupaMesmoIntervalo() throws Exception {
    Long sala = sala();
    var resultados = simultaneas(form("P6", sala), form("P6", sala));
    assertEquals(1, resultados.stream().filter(x -> x.equals("SEM_COTA")).count());
    assertEquals(1, resultados.stream().filter(x -> x.equals("CONFLITO")).count());
  }

  @Test
  void cotaCompartilhadaNaoEConsumidaDuasVezes() throws Exception {
    var p = personas.findByCodigo("P3").orElseThrow();
    p.cotaMensalHoras = new BigDecimal("6");
    personas.saveAndFlush(p);
    var resultados = simultaneas(form("P3", sala()), form("P3", sala()));
    assertEquals(1, resultados.stream().filter(x -> x.equals("COTA")).count());
    assertEquals(1, resultados.stream().filter(x -> x.equals("SEM_COTA")).count());
  }
}
