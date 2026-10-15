package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.service.*;
import java.math.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class RegrasTest {
  @Test
  void detectaSobreposicaoEMantemHorariosAdjacentesLivres() {
    assertTrue(
        DisponibilidadeService.sobrepoe(
            LocalTime.of(15, 0), LocalTime.of(17, 0), LocalTime.of(14, 0), LocalTime.of(16, 0)));
    assertFalse(
        DisponibilidadeService.sobrepoe(
            LocalTime.of(16, 0), LocalTime.of(17, 0), LocalTime.of(14, 0), LocalTime.of(16, 0)));
  }

  @Test
  void calculaSinal30() {
    assertEquals(new BigDecimal("480.00"), PrecoService.sinal(new BigDecimal("1600"), 30));
  }

  @Test
  void calculaSinal60() {
    assertEquals(new BigDecimal("960.00"), PrecoService.sinal(new BigDecimal("1600"), 60));
  }

  @Test
  void domingoEFeriadoUsam60EDiaComum30() {
    FeriadoRepository f = mock(FeriadoRepository.class);
    var p = new PrecoService(null, null, f, null, null);
    LocalDate feriado = LocalDate.of(2026, 10, 12);
    when(f.existsByDataAndAtivoTrue(feriado)).thenReturn(true);
    assertEquals(60, p.percentualSinal(LocalDate.of(2026, 10, 11)));
    assertEquals(60, p.percentualSinal(feriado));
    assertEquals(30, p.percentualSinal(LocalDate.of(2026, 10, 13)));
  }

  @Test
  void taxacaoRespeitaPersonas() {
    var t = new TaxacaoService();
    assertEquals(0, t.calcular("P1", false, true, false).locacao());
    assertEquals(50, t.calcular("P2", true, true, false).locacao());
    assertTrue(t.calcular("P2", true, false, true).insumos());
    for (String codigo : new String[] {"P3", "P4", "P5"}) {
      assertEquals(75, t.calcular(codigo, true, true, false).locacao());
      assertEquals(0, t.calcular(codigo, true, false, false).locacao());
      assertEquals(100, t.calcular(codigo, false, false, false).locacao());
    }
    assertEquals(100, t.calcular("P6", false, false, false).locacao());
  }
}
