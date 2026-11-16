package br.com.inpetu.hub.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** O formulário do pedido como foi digitado, os erros por campo (na ordem da tela) e os dados conferidos. */
class PedidoFormularioTest {

    static Map<String, String> campos() {
        Map<String, String> c = new HashMap<>();
        c.put("espacoId", "1");
        c.put("personaId", "6");
        c.put("data", "2026-11-24");
        c.put("horaInicio", "08:00");
        c.put("horaFim", "12:00");
        c.put("nome", "João D’Ávila");
        c.put("tipoDocumento", "CNPJ");
        c.put("documento", "12.345.678/0001-90");
        c.put("email", "joao@example.com");
        c.put("telefone", "48999991234");
        c.put("empresa", "Instituição");
        c.put("cidade", "São José");
        c.put("titulo", "Reunião");
        c.put("objetivo", "Conectar");
        c.put("numeroParticipantes", "10");
        return c;
    }

    @Test
    void dadosCertosViramOPedidoConferidoComDocumentoETelefoneNormalizados() {
        PedidoFormulario f = PedidoFormulario.de(campos()::get);
        PedidoConferido p = f.conferir();
        assertNotNull(p, f.getErros().toString());
        assertEquals(1L, p.espacoId());
        assertEquals(6L, p.personaId());
        assertEquals(LocalDate.of(2026, 11, 24), p.data());
        assertEquals(LocalTime.of(8, 0), p.inicio());
        assertEquals("12345678000190", p.documento());
        assertEquals("CNPJ", p.tipoDocumento());
        assertEquals("(48) 99999-1234", p.telefone());
        assertEquals(10, p.participantes());
        assertEquals(null, p.observacoes(), "campo opcional vazio vira nulo");
    }

    @Test
    void empresaEDocumentoSaoOpcionais() {
        Map<String, String> c = campos();
        c.put("empresa", "  ");
        c.put("tipoDocumento", "NENHUM");
        c.put("documento", "");
        PedidoConferido p = PedidoFormulario.de(c::get).conferir();
        assertNull(p.empresa());
        assertNull(p.documento());
        assertEquals("NENHUM", p.tipoDocumento());
    }

    @Test
    void errosFicamPorCampoNaOrdemDaTela() {
        Map<String, String> c = campos();
        c.put("titulo", "");
        c.put("nome", "João 123");
        c.put("email", "11231231@a");
        c.remove("personaId");
        c.put("data", "not-a-date");
        PedidoFormulario f = PedidoFormulario.de(c::get);
        assertNull(f.conferir());
        assertEquals(List.of("data", "nome", "email", "titulo", "personaId"), List.copyOf(f.getErros().keySet()));
        assertEquals("Confira a data, o horário ou o número informado.", f.getErros().get("data"));
        assertEquals("Informe um nome com letras, sem números.", f.getErros().get("nome"));
        assertEquals("Informe um email completo, como nome@instituicao.com.br.", f.getErros().get("email"));
        assertEquals("Preencha este campo.", f.getErros().get("titulo"));
        assertEquals("Preencha este campo.", f.getErros().get("personaId"));
        assertEquals("Persona declarada", PedidoFormulario.ROTULOS.get("personaId"));
        assertEquals("João 123", f.getNome(), "o que foi digitado volta para a tela");
    }

    @Test
    void documentoTelefoneTamanhoEQuantidadeSaoConferidos() {
        Map<String, String> c = campos();
        c.put("tipoDocumento", "CPF");
        c.put("documento", "1234");
        c.put("telefone", "123");
        c.put("numeroParticipantes", "0");
        c.put("objetivo", "x".repeat(2001));
        c.put("cidade", "x".repeat(256));
        PedidoFormulario f = PedidoFormulario.de(c::get);
        assertNull(f.conferir());
        assertEquals("Informe os 11 dígitos do CPF.", f.getErros().get("documento"));
        assertTrue(f.getErros().get("telefone").startsWith("Informe um telefone válido"));
        assertEquals("Informe um número inteiro a partir de 1.", f.getErros().get("numeroParticipantes"));
        assertEquals("Use até 2000 caracteres.", f.getErros().get("objetivo"));
        assertEquals("Use até 255 caracteres.", f.getErros().get("cidade"));
    }

    @Test
    void caixasDesmarcadasValemNao() {
        Map<String, String> c = campos();
        c.put("academico", "on");
        PedidoConferido p = PedidoFormulario.de(c::get).conferir();
        assertTrue(p.academico());
        assertEquals(false, p.inscricaoPaga());
        assertEquals(false, p.financiado());
    }
}
