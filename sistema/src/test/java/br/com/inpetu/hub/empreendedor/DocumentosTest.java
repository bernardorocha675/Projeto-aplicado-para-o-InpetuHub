package br.com.inpetu.hub.empreendedor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.empreendedor.Documentos.Documento;

/** Porta do DocumentosTest: CNPJ, CPF, documento estrangeiro ou sem documento; só a quantidade é conferida. */
class DocumentosTest {

    @Test
    void cadaTipoGuardaSoOQueAceita() {
        assertEquals(new Documento("CNPJ", "12345678000190"), Documentos.normalizar("CNPJ", "12.345.678/0001-90"));
        assertEquals(new Documento("CPF", "12345678909"), Documentos.normalizar("CPF", " 123.456.789-09 "));
        assertEquals(new Documento("ESTRANGEIRO", "AB-123456"), Documentos.normalizar("ESTRANGEIRO", " ab-123 456 "));
        assertEquals(new Documento("NENHUM", null), Documentos.normalizar("NENHUM", ""));
        assertEquals(new Documento("NENHUM", null), Documentos.normalizar("NENHUM", null));
    }

    @Test
    void semTipoValeComoCnpjOuSemDocumento() {
        assertEquals(new Documento("CNPJ", "12345678000190"), Documentos.normalizar(null, "12345678000190"));
        assertEquals(new Documento("CNPJ", "12345678000190"), Documentos.normalizar("", "12345678000190"));
        assertEquals(new Documento("NENHUM", null), Documentos.normalizar(null, ""));
    }

    @Test
    void numerosErradosSaoRecusados() {
        String[][] ruins = {{"CNPJ", "123"}, {"CNPJ", ""}, {"CNPJ", "1234567800019a"}, {"CPF", "1234567890"}, {"CPF",
                "123456789012"}, {"ESTRANGEIRO", "ab"}, {"ESTRANGEIRO", "x".repeat(31)}, {"ESTRANGEIRO", "AB<script>"},
                {"NENHUM", "123"}, {"RG", "1234567"}, {null, "123"}};
        for (String[] ruim : ruins) {
            assertThrows(RegraException.class, () -> Documentos.normalizar(ruim[0], ruim[1]), ruim[0] + " " + ruim[1]);
        }
    }

    @Test
    void mensagemNaoRepeteONomeDoCampo() {
        RegraException erro = assertThrows(RegraException.class, () -> Documentos.normalizar("CPF", ""));
        assertEquals("Informe o número do CPF.", erro.getMessage());
        assertEquals("Com “Sem documento”, deixe o número em branco.", assertThrows(RegraException.class,
                () -> Documentos.normalizar("NENHUM", "1")).getMessage());
    }

    @Test
    void telasMostramTipoENumero() {
        assertEquals("CPF 12345678909", Documentos.exibir("CPF", "12345678909"));
        assertEquals("CNPJ 12345678000190", Documentos.exibir(null, "12345678000190"));
        assertEquals("Documento estrangeiro AB123", Documentos.exibir("ESTRANGEIRO", "AB123"));
        assertEquals("Sem documento", Documentos.exibir("NENHUM", null));
    }
}
