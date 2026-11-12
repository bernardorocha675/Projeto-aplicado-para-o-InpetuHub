package br.com.inpetu.hub.telas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Cores só nos tokens (um único :root) e contraste WCAG AA calculado a partir do CSS. */
class TokensTest {

    // Cor escrita direto numa regra (fora do :root): só branco e sombras transparentes ficam de fora.
    private static final Pattern COR_SOLTA = Pattern.compile("#(?!fff\\b|ffffff\\b)[0-9a-fA-F]{3,8}\\b");

    @Test
    void umSoRootComOsTokensENenhumaCorSolta() throws Exception {
        List<Path> arquivos;
        try (Stream<Path> lista = Files.list(Css.PASTA)) {
            arquivos = lista.filter(p -> p.toString().endsWith(".css")).sorted().toList();
        }
        long roots = 0;
        for (Path arquivo : arquivos) {
            roots += Pattern.compile("(?m)^:root \\{").matcher(Files.readString(arquivo)).results().count();
        }
        assertEquals(1L, roots, "os tokens ficam num só :root (em tokens.css)");
        for (Path arquivo : arquivos) {
            if (arquivo.endsWith("tokens.css")) {
                continue;
            }
            String semSombras = Files.readString(arquivo).replaceAll("#[0-9a-fA-F]{6}[0-9a-fA-F]{2}\\b", "");
            Matcher m = COR_SOLTA.matcher(semSombras);
            List<String> soltas = new ArrayList<>();
            while (m.find()) {
                soltas.add(m.group());
            }
            assertTrue(soltas.isEmpty(), arquivo.getFileName() + " com cores fora dos tokens: " + soltas);
        }
    }

    @Test
    void textosEFundosComContrasteAA() throws Exception {
        String tokens = Css.arquivo("tokens.css");
        String branco = "#ffffff";
        String fundo = Css.variavel(tokens, "--bg");
        for (String texto : new String[]{"--ink", "--muted", "--navy"}) {
            String cor = Css.cor("var(" + texto + ")", tokens);
            Css.assertContraste(texto + " sobre branco", cor, branco, 4.5);
            Css.assertContraste(texto + " sobre --bg", cor, fundo, 4.5);
        }
        // O azul dos links passa só sobre branco (4,33:1 sobre --bg): por isso a página tem fundo branco.
        Css.assertContraste("--blue sobre branco", Css.cor("var(--blue)", tokens), branco, 4.5);
        assertEquals("#fff", Css.propriedade(Css.arquivo("base.css"), "body", "background"),
                "links azuis sobre fundo branco");
        Css.assertContraste("botão: branco sobre --blue", branco, Css.cor("var(--blue)", tokens), 4.5);
        for (String estado : new String[]{"erro", "sucesso", "aviso"}) {
            Css.assertContraste("aviso de " + estado, Css.cor("var(--" + estado + ")", tokens),
                    Css.variavel(tokens, "--" + estado + "-fundo"), 4.5);
        }
    }

    @Test
    void contornoDeFocoVisivelEPularParaOConteudo() throws Exception {
        String tokens = Css.arquivo("tokens.css");
        String base = Css.arquivo("base.css");
        String foco = Css.cor(Css.propriedade(base, ":focus-visible", "outline-color"), tokens);
        for (String fundo : new String[]{"#ffffff", Css.variavel(tokens, "--bg"), Css.cor("var(--blue)", tokens)}) {
            Css.assertContraste("contorno de foco", foco, fundo, 3);
        }
        assertFalse(Css.propriedade(base, ":focus-visible", "outline-width").startsWith("0"));
        String pular = Css.regra(base, ".pular");
        assertTrue(pular.contains("position: absolute"), pular);
        assertTrue(Css.regra(base, ".pular:focus").contains("top:"), "o link aparece ao receber o foco");
        Css.assertContraste("link de pular", Css.cor(Css.propriedade(base, ".pular", "color"), tokens), "#ffffff",
                4.5);
    }
}
