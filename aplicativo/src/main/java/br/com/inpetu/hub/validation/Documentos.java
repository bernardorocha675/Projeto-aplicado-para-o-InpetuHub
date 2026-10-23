package br.com.inpetu.hub.validation;

import br.com.inpetu.hub.exception.RegraException;
import java.util.*;

/**
 * Documento do empreendedor: CNPJ, CPF, documento estrangeiro ou nenhum. O número fica na coluna
 * cnpj (nome mantido para não mexer nos dados gravados) e o tipo em tipoDocumento. Só a quantidade
 * de dígitos é conferida, sem dígito verificador.
 */
public final class Documentos {
  private Documentos() {}

  public static final String CNPJ = "CNPJ", CPF = "CPF", ESTRANGEIRO = "ESTRANGEIRO", NENHUM = "NENHUM";

  public static final Map<String, String> TIPOS = tipos();

  private static Map<String, String> tipos() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put(CNPJ, "CNPJ");
    m.put(CPF, "CPF");
    m.put(ESTRANGEIRO, "Documento estrangeiro");
    m.put(NENHUM, "Sem documento");
    return Collections.unmodifiableMap(m);
  }

  /** Tipo e número prontos para gravar; sem documento, o número é null. */
  public record Documento(String tipo, String numero) {}

  public static Documento normalizar(String tipo, String numero) {
    String n = numero == null ? "" : numero.strip();
    // Sem tipo: formulário ou pedido de antes da escolha do documento, que só tinha CNPJ.
    String t = tipo == null || tipo.isBlank() ? (n.isEmpty() ? NENHUM : CNPJ) : tipo.strip();
    if (!TIPOS.containsKey(t)) throw new RegraException("Documento: escolha um tipo da lista.");
    if (t.equals(NENHUM)) {
      if (!n.isEmpty()) throw new RegraException("Documento: com “Sem documento”, deixe o número em branco.");
      return new Documento(NENHUM, null);
    }
    if (n.isEmpty())
      throw new RegraException(
          "Documento: informe o número do " + (t.equals(ESTRANGEIRO) ? "documento estrangeiro" : t) + ".");
    return switch (t) {
      case CNPJ -> new Documento(CNPJ, digitos(n, 14, "CNPJ: informe os 14 dígitos."));
      case CPF -> new Documento(CPF, digitos(n, 11, "CPF: informe os 11 dígitos."));
      default -> {
        String e = n.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (!e.matches("[A-Z0-9][A-Z0-9./\\x2d]{3,29}"))
          throw new RegraException(
              "Documento estrangeiro: use de 4 a 30 letras ou números (pontos, barras e hífens são aceitos).");
        yield new Documento(ESTRANGEIRO, e);
      }
    };
  }

  /** Texto para as telas internas, como “CPF 12345678909” ou “Sem documento”. */
  public static String exibir(String tipo, String numero) {
    if (numero == null || numero.isBlank()) return TIPOS.get(NENHUM);
    String t = tipo == null || tipo.isBlank() ? CNPJ : tipo;
    return TIPOS.getOrDefault(t, t) + " " + numero;
  }

  /** Aceita pontuação comum (12.345.678/0001-90) e grava só os dígitos. */
  private static String digitos(String n, int quantidade, String erro) {
    if (!n.matches("[0-9 ./\\x2d]+")) throw new RegraException(erro);
    String d = n.replaceAll("[^0-9]", "");
    if (d.length() != quantidade) throw new RegraException(erro);
    return d;
  }
}
