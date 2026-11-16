package br.com.inpetu.hub.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.AmbienteDeTeste;
import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.conta.Contas;

/**
 * O registro do pedido é idempotente pela chave e não toca no CRM nem na cota (porta do
 * publicIntakeDoesNotPoisonCrmOrReserveQuotaAndIsIdempotent); pedidos vencem em 24 h.
 */
class SolicitacaoServicoTest {

    private static PedidoConferido pedido(Ambiente ambiente) throws Exception {
        Map<String, String> c = PedidoFormularioTest.campos();
        c.put("espacoId",
                Contas.texto(ambiente.banco(), "select id_espaco from espaco where nome_espaco = 'Coworking'"));
        return PedidoFormulario.de(c::get).conferir();
    }

    @Test
    void mesmaChaveDevolveOMesmoPedidoSemDuplicar() throws Exception {
        Ambiente ambiente = AmbienteDeTeste.novo();
        SolicitacaoServico servico = new SolicitacaoServico(ambiente);
        PedidoConferido pedido = pedido(ambiente);
        String chave = "chave-de-teste-0000000001";
        String primeira = servico.registrar(pedido, chave);
        assertEquals(primeira, servico.registrar(pedido, chave));
        assertEquals("1", Contas.texto(ambiente.banco(), "select count(*) from solicitacao_publica"));
        assertEquals("0", Contas.texto(ambiente.banco(), "select count(*) from empreendedor"));
        assertEquals("0", Contas.texto(ambiente.banco(), "select count(*) from movimentacao_cota"));
        assertTrue(primeira.matches("[A-Za-z0-9_-]{32,64}"), primeira);
        String outra = servico.registrar(pedido, "chave-de-teste-0000000002");
        assertTrue(!outra.equals(primeira), "outra chave é outro pedido");
    }

    @Test
    void chaveInvalidaERecusada() throws Exception {
        Ambiente ambiente = AmbienteDeTeste.novo();
        SolicitacaoServico servico = new SolicitacaoServico(ambiente);
        PedidoConferido pedido = pedido(ambiente);
        for (String chave : new String[]{null, "curta", "com espaço 0000000000000", "x".repeat(65)}) {
            assertThrows(RegraException.class, () -> servico.registrar(pedido, chave), String.valueOf(chave));
        }
    }

    @Test
    void pedidoVenceEm24HorasEOVencimentoFicaNaAuditoria() throws Exception {
        AmbienteDeTeste.Relogio relogio = new AmbienteDeTeste.Relogio();
        Ambiente ambiente = AmbienteDeTeste.novo(false, relogio);
        SolicitacaoServico servico = new SolicitacaoServico(ambiente);
        servico.registrar(pedido(ambiente), "chave-de-teste-0000000003");
        relogio.adiantar(Duration.ofHours(23).plusMinutes(59));
        assertEquals(0, servico.expirarVencidas());
        assertEquals("RECEBIDA", Contas.texto(ambiente.banco(), "select estado from solicitacao_publica"));
        relogio.adiantar(Duration.ofMinutes(1));
        assertEquals(1, servico.expirarVencidas());
        assertEquals("EXPIRADA", Contas.texto(ambiente.banco(), "select estado from solicitacao_publica"));
        assertEquals(0, servico.expirarVencidas(), "só uma vez");
        List<String> auditoria = Contas.auditoria(ambiente.banco());
        assertTrue(auditoria.contains("SOLICITACAO_EXPIRADA OK null solicitacao"), auditoria.toString());
    }

    @Test
    void registrarConfereDeNovoOHorarioEOPreco() throws Exception {
        Ambiente ambiente = AmbienteDeTeste.novo();
        SolicitacaoServico servico = new SolicitacaoServico(ambiente);
        Contas.executar(ambiente.banco(), "update espaco set ativo = false");
        assertEquals("Este espaço está indisponível.", assertThrows(RegraException.class, () -> servico.registrar(
                pedido(ambiente), "chave-de-teste-0000000004")).getMessage());
        assertEquals("0", Contas.texto(ambiente.banco(), "select count(*) from solicitacao_publica"));
    }
}
