package br.com.inpetu.hub.service;

import java.util.*;

/**
 * Objetivos da visita (lote 5): opções fixas, mais de uma por visita, e “Outro” com texto livre.
 * Gravados no mesmo campo de texto de antes, como “Conhecer o Hub; Evento; Outro: …”, sem
 * migration; um objetivo antigo escrito à mão aparece em “Outro” ao editar e não se perde.
 */
public final class VisitaObjetivos {
  private VisitaObjetivos() {}

  public static final List<String> OPCOES =
      List.of(
          "Conhecer o Hub",
          "Reunião/parceria",
          "Evento",
          "Mentoria/apoio ao negócio",
          "Visita técnica/acadêmica");
  private static final String SEPARADOR = "; ";
  private static final String OUTRO = "Outro: ";

  /** Texto gravado a partir do formulário (objetivo__form, objetivo__0..4 marcados e objetivo__outro). */
  public static String compor(Map<String, String> dados, String campo) {
    // Envio sem as caixas (integrações e testes antigos): o texto é aceito como antes.
    if (!dados.containsKey(campo + "__form")) return dados.getOrDefault(campo, "").strip();
    List<String> partes = new ArrayList<>();
    for (int i = 0; i < OPCOES.size(); i++)
      if ("on".equals(dados.get(campo + "__" + i))) partes.add(OPCOES.get(i));
    String outro = dados.getOrDefault(campo + "__outro", "").replace(SEPARADOR, ", ").strip();
    if (outro.length() > 500) outro = outro.substring(0, 500);
    if (!outro.isEmpty()) partes.add(OUTRO + outro);
    return String.join(SEPARADOR, partes);
  }

  public static Set<String> marcados(String gravado) {
    Set<String> marcados = new LinkedHashSet<>();
    for (String parte : partes(gravado)) if (OPCOES.contains(parte)) marcados.add(parte);
    return marcados;
  }

  public static String outro(String gravado) {
    List<String> resto = new ArrayList<>();
    for (String parte : partes(gravado)) {
      if (OPCOES.contains(parte)) continue;
      resto.add(parte.startsWith(OUTRO) ? parte.substring(OUTRO.length()) : parte);
    }
    return String.join(", ", resto);
  }

  private static List<String> partes(String gravado) {
    if (gravado == null || gravado.isBlank()) return List.of();
    return Arrays.stream(gravado.split(SEPARADOR)).map(String::strip).filter(s -> !s.isEmpty()).toList();
  }
}
