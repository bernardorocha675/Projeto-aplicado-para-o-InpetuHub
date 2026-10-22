package br.com.inpetu.hub.validation;

import br.com.inpetu.hub.exception.RegraException;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.text.Collator;
import java.util.*;

/**
 * Telefone com país (libphonenumber): o número é conferido para o país escolhido ou pelo código
 * digitado após o +. Brasil fica no formato nacional de sempre, como (48) 99808-2043; outros
 * países no formato internacional, como +351 912 345 678.
 */
public final class Telefones {
  private Telefones() {}

  private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();
  private static final Locale PT_BR = new Locale("pt", "BR");
  public static final String PADRAO = "BR";

  /** País do seletor; principal marca o país usado quando vários dividem o código (ex.: +1). */
  public record Pais(String regiao, String nome, int codigo, boolean principal) {
    public String rotulo() {
      return nome + " (+" + codigo + ")";
    }
  }

  public static final List<Pais> PAISES = paises();

  private static List<Pais> paises() {
    Collator ordem = Collator.getInstance(PT_BR);
    List<Pais> lista = new ArrayList<>();
    for (String regiao : UTIL.getSupportedRegions()) {
      int codigo = UTIL.getCountryCodeForRegion(regiao);
      String nome = new Locale("", regiao).getDisplayCountry(PT_BR);
      lista.add(new Pais(regiao, nome, codigo, regiao.equals(UTIL.getRegionCodeForCountryCode(codigo))));
    }
    lista.sort((a, b) -> ordem.compare(a.nome(), b.nome()));
    // Brasil primeiro: é o caso mais comum no Hub.
    lista.sort(Comparator.comparing((Pais p) -> !p.regiao().equals(PADRAO)));
    return List.copyOf(lista);
  }

  public static String normalizar(String regiao, String bruto) {
    String numero = bruto == null ? "" : bruto.strip();
    if (numero.isEmpty()) throw new RegraException("Telefone: informe o número.");
    if (numero.length() > 30 || !numero.matches("\\+?[0-9() .\\x2d]+"))
      throw new RegraException("Telefone: use somente números, espaços, parênteses, hífen e o + do código do país.");
    String origem =
        numero.startsWith("+")
            ? "ZZ"
            : regiao != null && UTIL.getSupportedRegions().contains(regiao) ? regiao : PADRAO;
    PhoneNumber n;
    try {
      n = UTIL.parse(numero, origem);
    } catch (NumberParseException e) {
      throw invalido(origem);
    }
    if (!UTIL.isValidNumber(n)) throw invalido(origem.equals("ZZ") ? regiaoDe(n) : origem);
    return n.getCountryCode() == 55
        ? UTIL.format(n, PhoneNumberFormat.NATIONAL)
        : UTIL.format(n, PhoneNumberFormat.INTERNATIONAL);
  }

  /** Número já gravado: brasileiro sem + ou internacional com +. */
  public static boolean valido(String gravado) {
    try {
      normalizar(PADRAO, gravado);
      return true;
    } catch (RegraException e) {
      return false;
    }
  }

  /** País para preencher o seletor ao editar um número gravado. */
  public static String regiao(String gravado) {
    if (gravado == null || !gravado.strip().startsWith("+")) return PADRAO;
    try {
      String r = regiaoDe(UTIL.parse(gravado.strip(), "ZZ"));
      return r == null ? PADRAO : r;
    } catch (NumberParseException e) {
      return PADRAO;
    }
  }

  private static String regiaoDe(PhoneNumber n) {
    String r = UTIL.getRegionCodeForNumber(n);
    return r != null ? r : UTIL.getRegionCodeForCountryCode(n.getCountryCode());
  }

  private static RegraException invalido(String regiao) {
    String pais =
        regiao == null || regiao.equals("ZZ")
            ? "o país informado"
            : new Locale("", regiao).getDisplayCountry(PT_BR);
    return new RegraException(
        "Telefone inválido para "
            + pais
            + ". Confira o país e o número"
            + (PADRAO.equals(regiao) ? ", com DDD." : " (sem o zero inicial, se houver)."));
  }
}
