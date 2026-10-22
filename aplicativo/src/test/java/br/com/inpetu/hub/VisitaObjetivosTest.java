package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import br.com.inpetu.hub.service.VisitaObjetivos;
import java.util.*;
import org.junit.jupiter.api.Test;

// Lote 5: objetivos da visita como opções marcáveis e Outro, no mesmo campo de texto.
class VisitaObjetivosTest {
  @Test
  void marcadosEOutroViramUmTextoQueVoltaAoFormulario() {
    Map<String, String> dados = new HashMap<>();
    dados.put("objetivo__form", "1");
    dados.put("objetivo__0", "on");
    dados.put("objetivo__2", "on");
    dados.put("objetivo__outro", "  Gravar vídeo; entrevista ");
    String gravado = VisitaObjetivos.compor(dados, "objetivo");
    assertEquals("Conhecer o Hub; Evento; Outro: Gravar vídeo, entrevista", gravado);
    assertEquals(Set.of("Conhecer o Hub", "Evento"), VisitaObjetivos.marcados(gravado));
    assertEquals("Gravar vídeo, entrevista", VisitaObjetivos.outro(gravado));
  }

  @Test
  void nadaMarcadoFicaVazioParaOCampoObrigatorio() {
    assertEquals("", VisitaObjetivos.compor(Map.of("objetivo__form", "1", "objetivo__outro", " "), "objetivo"));
    assertEquals("", VisitaObjetivos.compor(Map.of("objetivo__form", "1", "objetivo__9", "on"), "objetivo"));
  }

  @Test
  void objetivoAntigoEscritoAMaoVaiParaOutroSemSePerder() {
    String antigo = "Apresentar o projeto ao reitor";
    assertTrue(VisitaObjetivos.marcados(antigo).isEmpty());
    assertEquals(antigo, VisitaObjetivos.outro(antigo));
    assertEquals(Set.of("Conhecer o Hub"), VisitaObjetivos.marcados("Conhecer o Hub"));
    // Envio sem as caixas continua aceito como texto.
    assertEquals(antigo, VisitaObjetivos.compor(Map.of("objetivo", " " + antigo + " "), "objetivo"));
  }
}
