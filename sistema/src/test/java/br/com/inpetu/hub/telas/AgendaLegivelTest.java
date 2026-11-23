package br.com.inpetu.hub.telas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Contraste WCAG AA da agenda (4,5:1 para texto) e alvos de 44 px. Porta do AgendaLegivelTest do antigo. */
class AgendaLegivelTest {

    private static final String BRANCO = "#ffffff";

    @Test
    void textosDaAgendaTemContrasteAA() throws Exception {
        String tokens = Css.arquivo("tokens.css");
        String agenda = Css.arquivo("agenda.css");
        String faixa = Css.cor(Css.propriedade(agenda, ".agenda-faixa", "background"), tokens);
        String calendario = Css.cor(Css.propriedade(agenda, ".agenda", "background"), tokens);
        String cinza = Css.cor("var(--muted)", tokens);
        Css.assertContraste("texto branco da faixa", Css.cor(Css.propriedade(agenda, ".agenda-faixa", "color"), tokens),
                faixa, 4.5);
        Css.assertContraste("dias da semana", Css.cor(Css.propriedade(agenda, ".semana", "color"), tokens),
                calendario, 4.5);
        Css.assertContraste("rótulos e dica do calendário", Css.cor(Css.propriedade(agenda, ".dica", "color"),
                tokens), calendario, 4.5);
        Css.assertContraste("texto cinza no detalhe do dia", cinza, BRANCO, 4.5);
        Css.assertContraste("dia com atividade", Css.cor("var(--ink)", tokens), Css.cor(Css.propriedade(agenda,
                ".calendario .dia.com-atividade", "background"), tokens), 4.5);
        Css.assertContraste("horário ocupado", Css.cor("var(--ink)", tokens), Css.cor(Css.propriedade(agenda,
                ".ocupado", "background"), tokens), 4.5);
        assertFalse(Css.regra(agenda, ".agenda-faixa .nota").contains("opacity"),
                "a transparência da nota reduz o contraste");
    }

    @Test
    void setasDiasEHorariosTemAlvoDe44px() throws Exception {
        String agenda = Css.arquivo("agenda.css");
        for (String seletor : new String[]{".seta", ".calendario .dia"}) {
            assertTrue(px(Css.propriedade(agenda, seletor, "min-height")) >= 44, seletor);
        }
        assertTrue(px(Css.propriedade(agenda, ".seta", "min-width")) >= 44);
        assertTrue(px(Css.propriedade(agenda, ".horario", "min-height")) >= 44);
    }

    private static double px(String valor) {
        assertTrue(valor.endsWith("px"), valor);
        return Double.parseDouble(valor.substring(0, valor.length() - 2));
    }
}
