package br.com.inpetu.hub.conta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.AmbienteDeTeste;

/** Porta do publicDemoCredentialsAreRestoredWhenApplicationStartsAgain: as 5 contas públicas voltam ao padrão. */
class ContasDemonstracaoTest {

    @Test
    void bancoNovoGanhaAsCincoContasComOsPerfisCertos() throws Exception {
        Ambiente ambiente = AmbienteDeTeste.novo(true);
        assertEquals(5, ContasDemonstracao.restaurar(ambiente));
        assertEquals("5", Contas.texto(ambiente.banco(), "select count(*) from colaborador"));
        for (ContasDemonstracao.Demonstracao conta : ContasDemonstracao.TODAS) {
            String perfil = Contas.texto(ambiente.banco(), "select perfil from colaborador where login = ?", conta
                    .login());
            assertEquals(conta.login().equals("bernardo") ? "ADMIN" : "COLABORADOR", perfil, conta.login());
            String hash = Contas.texto(ambiente.banco(), "select senha from colaborador where login = ?", conta
                    .login());
            assertTrue(hash.startsWith("$2a$12$"), "senha guardada só como BCrypt");
            assertTrue(Senhas.confere(conta.senha(), hash), conta.login());
        }
        assertEquals(List.of("CONTAS_ATUALIZADAS OK null contas"), Contas.auditoria(ambiente.banco()));
    }

    @Test
    void contaAlteradaVoltaAoPadraoAoIniciarDeNovo() throws Exception {
        Ambiente ambiente = AmbienteDeTeste.novo(true);
        ContasDemonstracao.restaurar(ambiente);
        String versao = Contas.texto(ambiente.banco(), "select security_version from colaborador where login = ?",
                "bernardo");
        Contas.executar(ambiente.banco(),
                "update colaborador set senha = ?, ativo = false, perfil = 'COLABORADOR' where login = 'bernardo'",
                Senhas.hash("troca-sintetica"));

        assertEquals(1, ContasDemonstracao.restaurar(ambiente), "só a conta alterada muda");

        assertEquals("ADMIN", Contas.texto(ambiente.banco(), "select perfil from colaborador where login = ?",
                "bernardo"));
        assertEquals("TRUE", Contas.texto(ambiente.banco(), "select ativo from colaborador where login = ?",
                "bernardo"));
        assertTrue(Senhas.confere("bernardo040", Contas.texto(ambiente.banco(),
                "select senha from colaborador where login = ?", "bernardo")));
        assertEquals(Long.parseLong(versao) + 1, Long.parseLong(Contas.texto(ambiente.banco(),
                "select security_version from colaborador where login = ?", "bernardo")),
                "a versão sobe e derruba as sessões abertas");
    }

    @Test
    void semAlteracaoNadaMudaNemEAuditado() throws Exception {
        Ambiente ambiente = AmbienteDeTeste.novo(true);
        ContasDemonstracao.restaurar(ambiente);
        assertEquals(0, ContasDemonstracao.restaurar(ambiente));
        assertEquals(1, Contas.auditoria(ambiente.banco()).size());
    }
}
