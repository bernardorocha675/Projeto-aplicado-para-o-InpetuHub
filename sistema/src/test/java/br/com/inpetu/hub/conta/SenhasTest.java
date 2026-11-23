package br.com.inpetu.hub.conta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** BCrypt com custo 12, como o antigo, e as regras da nova senha. */
class SenhasTest {

    @Test
    void bcryptComCusto12() {
        String hash = Senhas.hash("Teste123");
        assertTrue(hash.startsWith("$2a$12$"), hash);
        assertTrue(Senhas.confere("Teste123", hash));
        assertFalse(Senhas.confere("teste123", hash));
        assertFalse(Senhas.confere("Teste123", null), "conta sem senha nunca entra");
        assertFalse(Senhas.confere("Teste123", "não é bcrypt"));
    }

    @Test
    void novaSenhaDe8a64CaracteresEAte72Bytes() {
        assertNull(Senhas.problemaDaNova("Teste123"), "8 caracteres simples bastam");
        assertNull(Senhas.problemaDaNova("x".repeat(64)));
        assertEquals(Senhas.TAMANHO, Senhas.problemaDaNova("Curta12"));
        assertEquals(Senhas.TAMANHO, Senhas.problemaDaNova("x".repeat(65)));
        assertEquals(Senhas.TAMANHO, Senhas.problemaDaNova("ç".repeat(37)), "37 caracteres, 74 bytes");
        assertEquals(Senhas.TAMANHO, Senhas.problemaDaNova(null));
    }
}
