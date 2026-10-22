package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.validation.Documentos;
import br.com.inpetu.hub.validation.Documentos.Documento;
import org.junit.jupiter.api.Test;

// Documento do empreendedor: CNPJ, CPF, documento estrangeiro ou sem documento.
class DocumentosTest {
  @Test
  void eachTypeKeepsOnlyWhatItAccepts() {
    assertEquals(new Documento("CNPJ", "12345678000190"), Documentos.normalizar("CNPJ", "12.345.678/0001-90"));
    assertEquals(new Documento("CPF", "12345678909"), Documentos.normalizar("CPF", " 123.456.789-09 "));
    assertEquals(new Documento("ESTRANGEIRO", "AB-123456"), Documentos.normalizar("ESTRANGEIRO", " ab-123 456 "));
    assertEquals(new Documento("NENHUM", null), Documentos.normalizar("NENHUM", ""));
    assertEquals(new Documento("NENHUM", null), Documentos.normalizar("NENHUM", null));
  }

  @Test
  void requestsAndFormsWithoutTypeAreTheOldCnpjOnes() {
    assertEquals(new Documento("CNPJ", "12345678000190"), Documentos.normalizar(null, "12345678000190"));
    assertEquals(new Documento("CNPJ", "12345678000190"), Documentos.normalizar("", "12345678000190"));
    assertEquals(new Documento("NENHUM", null), Documentos.normalizar(null, ""));
  }

  @Test
  void wrongNumbersAreRefused() {
    for (String[] ruim :
        new String[][] {
          {"CNPJ", "123"}, {"CNPJ", ""}, {"CNPJ", "1234567800019a"}, {"CPF", "1234567890"},
          {"CPF", "123456789012"}, {"ESTRANGEIRO", "ab"}, {"ESTRANGEIRO", "x".repeat(31)},
          {"ESTRANGEIRO", "AB<script>"}, {"NENHUM", "123"}, {"RG", "1234567"}, {null, "123"}
        })
      assertThrows(RegraException.class, () -> Documentos.normalizar(ruim[0], ruim[1]), ruim[0] + " " + ruim[1]);
  }

  @Test
  void internalScreensShowTypeAndNumber() {
    assertEquals("CPF 12345678909", Documentos.exibir("CPF", "12345678909"));
    assertEquals("CNPJ 12345678000190", Documentos.exibir(null, "12345678000190"));
    assertEquals("Documento estrangeiro AB123", Documentos.exibir("ESTRANGEIRO", "AB123"));
    assertEquals("Sem documento", Documentos.exibir("NENHUM", null));
  }
}
