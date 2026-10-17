package br.com.inpetu.hub.service;

import org.springframework.stereotype.Service;

@Service
public class TaxacaoService {
  public record Taxas(int locacao, int operacao, int limpeza, boolean insumos) {}

  public Taxas calcular(String codigo, boolean cota, boolean pago, boolean academico) {
    if (codigo.equals("P1")) return new Taxas(0, 0, 0, false);
    if (!cota) return new Taxas(100, 100, 100, false);
    if (codigo.equals("P2")) {
      if (pago) return new Taxas(50, 100, 100, false);
      if (academico) return new Taxas(0, 0, 100, true);
      return new Taxas(100, 100, 100, false);
    }
    return new Taxas(pago ? 75 : 0, 100, 100, false);
  }
}
