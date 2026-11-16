package br.com.inpetu.hub.interna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.AmbienteDeTeste;
import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.conta.Contas;
import br.com.inpetu.hub.conta.Perfil;
import br.com.inpetu.hub.reserva.ReservaServico;
import br.com.inpetu.hub.reserva.SituacaoPagamento;
import br.com.inpetu.hub.solicitacao.SolicitacaoServico;

/**
 * Aceitar e recusar pedidos e as ações da reserva gravadas no banco, com histórico, cota e auditoria. Porta do restante
 * do FluxosIntegrationTest e dos testes de aceite do SecurityIntegrationTest.
 */
class ReservaServicoTest {

    private final AmbienteDeTeste.Relogio relogio = new AmbienteDeTeste.Relogio();
    private Ambiente ambiente;
    private SolicitacaoServico solicitacoes;
    private ReservaServico reservas;
    private long admin;
    private long colaborador;

    @BeforeEach
    void preparar() throws Exception {
        ambiente = AmbienteDeTeste.novo(false, relogio);
        Cenario.taxasZeradas(ambiente);
        solicitacoes = new SolicitacaoServico(ambiente);
        reservas = new ReservaServico(ambiente);
        admin = Contas.criar(ambiente.banco(), Perfil.ADMIN).id();
        colaborador = Contas.criar(ambiente.banco(), Perfil.COLABORADOR).id();
    }

    private String texto(String consulta, Object... valores) throws Exception {
        return Contas.texto(ambiente.banco(), consulta, valores);
    }

    private long aceitar(Map<String, String> campos) throws Exception {
        return solicitacoes.aceitar(Cenario.pedido(ambiente, campos), colaborador, true,
                "Confirmado por telefone com a coordenação.");
    }

    @Test
    void aceitarCriaACadastroEAReservaPendenteSemOcuparCota() throws Exception {
        long pedido = Cenario.pedido(ambiente, Cenario.campos(ambiente));
        long reserva = solicitacoes.aceitar(pedido, colaborador, true, "Confirmado com a coordenação.");
        assertEquals("PENDENTE", texto("select status_reserva from reserva where id_reserva = ?", reserva));
        assertEquals("PENDENTE", texto("select status_pagamento from reserva where id_reserva = ?", reserva));
        assertEquals("ENCAMINHADA", texto("select estado from solicitacao_publica where id = ?", pedido));
        assertEquals(String.valueOf(reserva), texto("select reserva_id from solicitacao_publica where id = ?", pedido));
        assertEquals("Maria Souza", texto("select nome from empreendedor"));
        assertEquals("12345678000190", texto("select documento from empreendedor"));
        assertEquals("0", texto("select count(*) from movimentacao_cota"));
        assertEquals("SOLICITAÇÃO", texto("select acao from historico_reserva where reserva_id = ?", reserva));
        assertTrue(Contas.auditoria(ambiente.banco()).contains("IDENTIDADE_CONFERIDA OK " + colaborador
                + " solicitacao"));
    }

    @Test
    void aceitarExigeAConferenciaEOMotivoEUmPedidoAindaRecebido() throws Exception {
        long pedido = Cenario.pedido(ambiente, Cenario.campos(ambiente));
        String mensagem = "Confira a identidade e informe como o vínculo com a instituição foi verificado.";
        assertEquals(mensagem, assertThrows(RegraException.class, () -> solicitacoes.aceitar(pedido, colaborador,
                false, "ok")).getMessage());
        assertThrows(RegraException.class, () -> solicitacoes.aceitar(pedido, colaborador, true, " "));
        assertThrows(RegraException.class, () -> solicitacoes.aceitar(pedido, colaborador, true, "x".repeat(2001)));
        assertEquals("0", texto("select count(*) from reserva"), "nada gravado");
        solicitacoes.recusar(pedido, colaborador);
        assertEquals("RECUSADA", texto("select estado from solicitacao_publica where id = ?", pedido));
        assertEquals("Solicitação processada ou expirada.", assertThrows(RegraException.class, () -> solicitacoes
                .aceitar(pedido, colaborador, true, "ok")).getMessage());
        assertThrows(RegraException.class, () -> solicitacoes.recusar(pedido, colaborador));
        assertTrue(Contas.auditoria(ambiente.banco()).contains("SOLICITACAO_RECUSADA OK " + colaborador
                + " solicitacao"));
    }

    @Test
    void pedidoVencidoNaoPodeSerAceito() throws Exception {
        long pedido = Cenario.pedido(ambiente, Cenario.campos(ambiente));
        relogio.adiantar(Duration.ofHours(25));
        assertThrows(RegraException.class, () -> solicitacoes.aceitar(pedido, colaborador, true, "ok"));
    }

    @Test
    void cadastroEAchadoPeloDocumentoOuSemDocumentoPeloEmailSemSobrescrever() throws Exception {
        aceitar(Cenario.campos(ambiente));
        Map<String, String> mesmoDocumento = Cenario.campos(ambiente);
        mesmoDocumento.put("nome", "Outro Nome");
        mesmoDocumento.put("data", "2026-11-25");
        aceitar(mesmoDocumento);
        assertEquals("1", texto("select count(*) from empreendedor"));
        assertEquals("Maria Souza", texto("select nome from empreendedor"), "não sobrescreve o cadastro");

        Map<String, String> semDocumento = Cenario.campos(ambiente);
        semDocumento.put("tipoDocumento", "NENHUM");
        semDocumento.put("documento", "");
        semDocumento.put("email", "ana@example.com");
        semDocumento.put("data", "2026-11-26");
        long primeira = aceitar(semDocumento);
        semDocumento.put("email", "ANA@example.com");
        semDocumento.put("data", "2026-11-27");
        long segunda = aceitar(semDocumento);
        assertEquals("2", texto("select count(*) from empreendedor"));
        assertEquals(texto("select id_empreendedor from reserva where id_reserva = ?", primeira), texto(
                "select id_empreendedor from reserva where id_reserva = ?", segunda), "mesmo email, mesmo cadastro");
    }

    @Test
    void cadastroInativoNaoRecebeReserva() throws Exception {
        aceitar(Cenario.campos(ambiente));
        Contas.executar(ambiente.banco(), "update empreendedor set ativo = false");
        Map<String, String> c = Cenario.campos(ambiente);
        c.put("data", "2026-11-25");
        long pedido = Cenario.pedido(ambiente, c);
        assertEquals("Cadastro inativo. Entre em contato com a equipe.", assertThrows(RegraException.class,
                () -> solicitacoes.aceitar(pedido, colaborador, true, "ok")).getMessage());
    }

    @Test
    void aprovarOcupaOHorarioEOutraPendenteDoMesmoHorarioNaoPassa() throws Exception {
        long primeira = aceitar(Cenario.campos(ambiente));
        long segunda = aceitar(Cenario.campos(ambiente));
        reservas.aprovar(primeira, admin);
        assertEquals("APROVADA", texto("select status_reserva from reserva where id_reserva = ?", primeira));
        assertEquals(String.valueOf(admin), texto("select id_colaborador_responsavel from reserva where id_reserva = ?",
                primeira));
        assertEquals("Horário indisponível: já existe uma reserva neste intervalo.", assertThrows(RegraException.class,
                () -> reservas.aprovar(segunda, admin)).getMessage());
        assertEquals("PENDENTE", texto("select status_reserva from reserva where id_reserva = ?", segunda));
        assertEquals("Horário indisponível: já existe uma reserva neste intervalo.", assertThrows(RegraException.class,
                () -> aceitar(Cenario.campos(ambiente))).getMessage(), "o aceite também confere a agenda");
    }

    @Test
    void aprovarComCotaGravaOMovimentoECancelarTardeCobraEmDobro() throws Exception {
        Contas.executar(ambiente.banco(), "update persona set cota_mensal_horas = 6 where codigo = 'P4'");
        Map<String, String> c = Cenario.campos(ambiente);
        c.put("personaId", String.valueOf(Cenario.persona(ambiente, "P4")));
        c.put("data", "2026-11-30");
        long reserva = aceitar(c);
        reservas.aprovar(reserva, admin);
        assertEquals("true",
                texto("select lower(cast(usa_cota as varchar)) from reserva where id_reserva = ?", reserva));
        assertEquals("UTILIZACAO", texto("select tipo from movimentacao_cota"));
        reservas.cancelar(reserva, false, "Desistência", admin, false);
        assertEquals("CANCELADA", texto("select status_reserva from reserva where id_reserva = ?", reserva));
        assertEquals("8.00", texto("select sum(quantidade_horas) from movimentacao_cota"),
                "4 usadas, 4 devolvidas e 8 de penalidade");
        assertEquals("Penalidade: 8.00h. Desistência", texto("select descricao from historico_reserva where acao ="
                + " 'CANCELAMENTO'"));
    }

    @Test
    void recusarAlterarPagamentoEConcluirFicamNoHistorico() throws Exception {
        long recusada = aceitar(Cenario.campos(ambiente));
        reservas.recusar(recusada, "Documentação incompleta", colaborador);
        assertEquals("Documentação incompleta", texto("select motivo_recusa from reserva where id_reserva = ?",
                recusada));

        long reserva = aceitar(Cenario.campos(ambiente));
        reservas.alterar(reserva, LocalDate.of(2026, 11, 29), LocalTime.of(8, 0), LocalTime.of(12, 0), Cenario.espaco(
                ambiente, "Coworking"), Cenario.persona(ambiente, "P6"), colaborador);
        assertEquals("60", texto("select percentual_sinal from reserva where id_reserva = ?", reserva), "domingo");
        assertTrue(texto("select descricao from historico_reserva where acao = 'ALTERAÇÃO'").contains(
                "Sala Maker VIA")
                && texto("select descricao from historico_reserva where acao = 'ALTERAÇÃO'").contains(
                        "Coworking"),
                "o histórico mostra os espaços");
        reservas.aprovar(reserva, admin);
        reservas.registrarPagamento(reserva, SituacaoPagamento.SINAL_PAGO, colaborador);
        assertEquals("SINAL_PAGO", texto("select status_pagamento from reserva where id_reserva = ?", reserva));
        assertThrows(RegraException.class, () -> reservas.concluir(reserva, colaborador), "ainda não aconteceu");
        relogio.adiantar(Duration.ofDays(18));
        reservas.concluir(reserva, colaborador);
        assertEquals("CONCLUIDA", texto("select status_reserva from reserva where id_reserva = ?", reserva));
        List<String> acoes = List.of(texto("select listagg(acao, ',') within group (order by id) from"
                + " historico_reserva where reserva_id = ?", reserva).split(","));
        assertEquals(List.of("SOLICITAÇÃO", "ALTERAÇÃO", "APROVAÇÃO", "PAGAMENTO", "CONCLUSÃO"), acoes);
        assertTrue(Contas.auditoria(ambiente.banco()).stream().anyMatch(l -> l.startsWith("RESERVA_APROVADA OK "
                + admin)));
    }

    @Test
    void forcaMaiorSoDoGestorETaxasAusentesImpedemAprovar() throws Exception {
        long reserva = aceitar(Cenario.campos(ambiente));
        assertEquals("A exceção exige um gestor e justificativa.", assertThrows(RegraException.class, () -> reservas
                .cancelar(reserva, true, "Interdição", colaborador, false)).getMessage());
        Contas.executar(ambiente.banco(), "update configuracao set taxa_limpeza = null");
        assertEquals("Configure os valores de operação/limpeza na Administração antes de aprovar.", assertThrows(
                RegraException.class, () -> reservas.aprovar(reserva, admin)).getMessage());
        reservas.cancelar(reserva, true, "Interdição registrada", admin, true);
        assertEquals("CANCELADA", texto("select status_reserva from reserva where id_reserva = ?", reserva));
    }

    @Test
    void reservaInexistenteDaMensagem() {
        assertEquals("Reserva não encontrada.", assertThrows(RegraException.class, () -> reservas.aprovar(999, admin))
                .getMessage());
    }

    @Test
    void pendenteVenceEm24Horas() throws Exception {
        long reserva = aceitar(Cenario.campos(ambiente));
        relogio.adiantar(Duration.ofHours(24));
        assertEquals(1, reservas.expirarPendentes());
        assertEquals("CANCELADA", texto("select status_reserva from reserva where id_reserva = ?", reserva));
        assertEquals(0, reservas.expirarPendentes());
        assertTrue(Contas.auditoria(ambiente.banco()).contains("PENDENTE_EXPIRADA OK null reservas"));
    }

    @Test
    void aprovacaoDeOutroEspacoNaoAfetaEAgendaPublicaMostraOcupado() throws Exception {
        long reserva = aceitar(Cenario.campos(ambiente));
        Map<String, String> outra = Cenario.campos(ambiente);
        outra.put("espacoId", String.valueOf(Cenario.espaco(ambiente, "Coworking")));
        long naOutraSala = aceitar(outra);
        reservas.aprovar(reserva, admin);
        reservas.aprovar(naOutraSala, admin);
        assertNotEquals(reserva, naOutraSala);
        assertEquals("2", texto("select count(*) from reserva where status_reserva = 'APROVADA'"));
    }
}
