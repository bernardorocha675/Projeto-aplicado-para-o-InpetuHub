package br.com.inpetu.hub.empreendedor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.inpetu.hub.comum.RegraException;

/** Nome sem números, email completo e telefone pela regra de dígitos (sem libphonenumber, decisão 3b). */
class ContatosTest {

    @Test
    void nomeAceitaLetrasAcentosApostrofoEHifen() {
        for (String nome : new String[]{"João D’Ávila", "Maria-José", "Ana O'Neil", "Zoë"}) {
            assertTrue(Contatos.nomeValido(nome), nome);
        }
        for (String nome : new String[]{"123134142", "João 123", " ", "-Ana", ""}) {
            assertFalse(Contatos.nomeValido(nome), nome);
        }
    }

    @Test
    void emailPrecisaDeDominioCompleto() {
        assertTrue(Contatos.emailValido("joao@example.com"));
        assertTrue(Contatos.emailValido("pessoa@instituicao.edu.br"));
        for (String email : new String[]{"11231231@a", "joao@example..com", "pessoa@dominio.1", "pessoa@dominio",
                "sem arroba.com", "a b@c.com"}) {
            assertFalse(Contatos.emailValido(email), email);
        }
    }

    @Test
    void telefoneDoBrasilGanhaAMascaraEOutroPaisFicaComMaisECodigo() {
        assertEquals("(48) 99999-1234", Contatos.telefone("48999991234"));
        assertEquals("(48) 99999-1234", Contatos.telefone("(48) 99999-1234"));
        assertEquals("(48) 3333-1234", Contatos.telefone("48 3333 1234"));
        assertEquals("+55 48 99999 1234", Contatos.telefone("+55 48 99999 1234"));
        assertEquals("+351 912 345 678", Contatos.telefone("+351 912 345 678"));
        assertEquals("004899991234567", Contatos.telefone("004899991234567"), "15 dígitos sem + ficam como vieram");
    }

    @Test
    void telefoneComLetrasOuQuantidadeErradaERecusado() {
        for (String telefone : new String[]{"123", "123123adwdawd", "+1234567", "4899999", "+1234567890123456",
                "1234567890123456", "(48) 9999-12a4", ""}) {
            assertThrows(RegraException.class, () -> Contatos.telefone(telefone), telefone);
        }
    }
}
