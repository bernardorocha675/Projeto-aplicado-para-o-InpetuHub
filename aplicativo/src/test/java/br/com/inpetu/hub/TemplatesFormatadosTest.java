package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Templates e CSS ficam formatados, um elemento ou regra por linha, para que as mudanças de tela sejam revisáveis. */
class TemplatesFormatadosTest {
  private static final int LIMITE = 160;
  // Um único atributo longo (expressão do Thymeleaf, caminho de ícone SVG) não tem onde quebrar.
  private static final Pattern ATRIBUTO_SOZINHO = Pattern.compile("\\s*[\\w:-]+=\"[^\"]*\"\\s*/?>?");

  @Test
  void nenhumaLinhaCompactada() throws Exception {
    List<String> longas;
    try (Stream<Path> arquivos =
        Stream.concat(
            Files.walk(Path.of("src/main/resources/templates")),
            Files.walk(Path.of("src/main/resources/static/css")))) {
      longas =
          arquivos
              .filter(p -> p.toString().endsWith(".html") || p.toString().endsWith(".css"))
              .flatMap(TemplatesFormatadosTest::linhasLongas)
              .toList();
    }
    assertTrue(longas.isEmpty(), "Linhas compactadas (formate com o Prettier, ver docs/ESTRUTURA.md):\n" + String.join("\n", longas));
  }

  private static Stream<String> linhasLongas(Path arquivo) {
    try {
      List<String> linhas = Files.readAllLines(arquivo);
      return java.util.stream.IntStream.range(0, linhas.size())
          .filter(i -> linhas.get(i).length() > LIMITE && !ATRIBUTO_SOZINHO.matcher(linhas.get(i)).matches())
          .mapToObj(i -> arquivo + ":" + (i + 1) + " (" + linhas.get(i).length() + " caracteres)");
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }
}
