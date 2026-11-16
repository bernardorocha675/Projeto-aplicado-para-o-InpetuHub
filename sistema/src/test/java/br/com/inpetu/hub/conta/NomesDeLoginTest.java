package br.com.inpetu.hub.conta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** O usuário aceita maiúsculas, acentos e espaços colados; a senha fica exatamente como foi digitada. */
class NomesDeLoginTest {

    @Test
    void normalizaMaiusculasAcentosEEspacos() {
        assertEquals("alunoteste", NomesDeLogin.normalizar("  ÁLUNÓ Teste "));
        assertEquals("joaof", NomesDeLogin.normalizar("JoãoF"));
        assertEquals("", NomesDeLogin.normalizar(null));
    }

    @Test
    void formatoUnicoDeUsuario() {
        assertTrue(NomesDeLogin.valido("bernardo"));
        assertTrue(NomesDeLogin.valido("pessoa.email@hub"));
        assertFalse(NomesDeLogin.valido("abc"), "mínimo de 4 caracteres");
        assertFalse(NomesDeLogin.valido("com espaço"));
        assertFalse(NomesDeLogin.valido("x".repeat(121)));
    }
}
