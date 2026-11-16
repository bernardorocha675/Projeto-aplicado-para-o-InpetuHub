package br.com.inpetu.hub.telas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Porta do AreaInternaTest (só computador, decisão 14): contraste do menu interno, das situações e dos avisos. */
class AreaInternaLegivelTest {

    private static final Path TELAS = Path.of("src/main/webapp/WEB-INF");

    @Test
    void menuInternoTabelasESituacoesComContrasteAA() throws Exception {
        String tokens = Css.arquivo("tokens.css");
        String interna = Css.arquivo("interna.css");
        String base = Css.arquivo("base.css");
        Css.assertContraste("menu interno", Css.cor(Css.propriedade(interna, ".menu-interno a", "color"), tokens),
                "#ffffff", 4.5);
        Css.assertContraste("item atual do menu interno", Css.cor(Css.propriedade(interna,
                ".menu-interno a[aria-current=\"page\"]", "color"), tokens), Css.cor(
                        Css.propriedade(interna,
                                ".menu-interno a[aria-current=\"page\"]", "background"),
                        tokens),
                4.5);
        Css.assertContraste("cabeçalho das tabelas", Css.cor(Css.propriedade(base, ".tabela thead th", "color"),
                tokens), Css.cor(Css.propriedade(base, ".tabela thead th", "background"), tokens), 4.5);
        for (String situacao : new String[]{"PENDENTE", "APROVADA", "RECUSADA", "CANCELADA", "CONCLUIDA"}) {
            String regra = ".situacao." + situacao;
            Css.assertContraste("situação " + situacao, Css.cor(Css.propriedade(interna, regra, "color"), tokens), Css
                    .cor(Css.propriedade(interna, regra, "background"), tokens), 4.5);
        }
        String rodape = Css.arquivo("base.css");
        Css.assertContraste("rodapé", "#ffffff", Css.cor(Css.propriedade(rodape, ".rodape", "background"), tokens),
                4.5);
    }

    @Test
    void avisosSaoAnunciados() throws Exception {
        List<Path> telas;
        try (Stream<Path> arquivos = Files.walk(TELAS)) {
            telas = arquivos.filter(p -> p.toString().endsWith(".jsp") || p.toString().endsWith(".tag")).toList();
        }
        Pattern aviso = Pattern.compile("<(p|div)[^>]*class=\"alerta ([a-z${}A-Z.]+)[^\"]*\"[^>]*>");
        for (Path tela : telas) {
            Matcher m = aviso.matcher(Files.readString(tela));
            while (m.find()) {
                String tag = m.group();
                boolean erro = m.group(2).equals("erro");
                assertTrue(erro ? tag.contains("role=\"alert\"") : tag.contains("role="), tela + ": " + tag);
            }
        }
    }
}
