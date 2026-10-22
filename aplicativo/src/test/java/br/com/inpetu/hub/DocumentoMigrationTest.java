package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

// Banco já em uso (0.5.6 ou anterior): a migration do documento só acrescenta e afrouxa colunas.
class DocumentoMigrationTest {
  @Test
  void existingEntrepreneursAndRequestsKeepTheirCnpjAndDocumentBecomesOptional() throws Exception {
    String url = "jdbc:h2:mem:documento_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    Flyway.configure().dataSource(url, "sa", "").target("5").load().migrate();
    try (Connection c = DriverManager.getConnection(url, "sa", "");
        Statement s = c.createStatement()) {
      s.execute(
          "INSERT INTO empreendedor (cnpj, nome, email, telefone, cidade, empresa, ativo) VALUES"
              + " ('12345678000190', 'Maria', 'maria@example.com', '(48) 99999-9999', 'Florianópolis', 'Empresa', TRUE)");
      s.execute(
          "INSERT INTO solicitacao_publica (referencia, chave, nome, cnpj, email, numero_participantes,"
              + " inscricao_paga, financiado, academico, criada_em, expira_em, email_confirmado, tentativas, estado)"
              + " VALUES ('ref-antiga', 'chave-antiga-0000', 'Maria', '12345678000190', 'maria@example.com', 1,"
              + " FALSE, FALSE, FALSE, NOW(), NOW(), FALSE, 0, 'RECEBIDA')");
      Flyway.configure().dataSource(url, "sa", "").load().migrate();
      try (ResultSet r =
          s.executeQuery("SELECT cnpj, tipo_documento, nome, email FROM empreendedor")) {
        assertTrue(r.next());
        assertEquals("12345678000190", r.getString(1));
        assertEquals("CNPJ", r.getString(2));
        assertEquals("Maria", r.getString(3));
        assertEquals("maria@example.com", r.getString(4));
        assertFalse(r.next());
      }
      try (ResultSet r = s.executeQuery("SELECT cnpj, tipo_documento FROM solicitacao_publica")) {
        assertTrue(r.next());
        assertEquals("12345678000190", r.getString(1));
        assertEquals("CNPJ", r.getString(2));
      }
      // Sem documento: vários cadastros com número vazio; documento estrangeiro de até 30 caracteres.
      s.execute(
          "INSERT INTO empreendedor (cnpj, tipo_documento, nome, email, ativo) VALUES"
              + " (NULL, 'NENHUM', 'Ana', 'ana@example.com', TRUE), (NULL, 'NENHUM', 'Bia', 'bia@example.com', TRUE),"
              + " ('" + "A".repeat(30) + "', 'ESTRANGEIRO', 'John', 'john@example.com', TRUE)");
      // O mesmo número continua único.
      assertThrows(
          SQLException.class,
          () ->
              s.execute(
                  "INSERT INTO empreendedor (cnpj, tipo_documento, nome, email, ativo) VALUES"
                      + " ('12345678000190', 'CNPJ', 'Outra', 'outra@example.com', TRUE)"));
    }
  }
}
