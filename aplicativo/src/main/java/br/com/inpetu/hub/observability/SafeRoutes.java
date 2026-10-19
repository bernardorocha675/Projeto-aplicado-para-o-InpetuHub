package br.com.inpetu.hub.observability;

import java.util.Set;

/** Registra o tipo da tela/ação, sem IDs, parâmetros ou caminhos fornecidos livremente. */
public final class SafeRoutes {
  private SafeRoutes() {}

  private static final Set<String> PUBLIC = Set.of(
      "/", "/agenda", "/login", "/logout", "/disponibilidade", "/reserva",
      "/reservas/resumo", "/reservas/solicitar", "/reservas/sucesso",
      "/solicitacoes/confirmar", "/solicitacoes/reenviar", "/admin", "/admin/reservas",
      "/admin/config", "/admin/config/cotas", "/admin/config/auditoria", "/admin/solicitacoes", "/conta/senha");

  public static String canonical(String path) {
    if (path == null || path.length() > 200) return "OTHER";
    for (String route : PUBLIC) if (route.equals(path)) return route;
    if (path.startsWith("/js/") || path.startsWith("/css/") || path.startsWith("/img/")
        || path.startsWith("/images/")
        || path.startsWith("/fonts/")
        || path.equals("/favicon.ico")) return "ASSET";
    if (path.matches("/admin/empreendedores/[0-9]{1,19}")) return "/admin/empreendedores/{id}";
    if (path.matches("/admin/reservas/[0-9]{1,19}")) return "/admin/reservas/{id}";
    for (String action : Set.of("aprovar", "recusar", "cancelar", "alterar", "pagamento", "concluir"))
      if (path.matches("/admin/reservas/[0-9]{1,19}/" + action))
        return "/admin/reservas/{id}/" + action;
    for (String action : Set.of("aceitar", "recusar"))
      if (path.matches("/admin/solicitacoes/[0-9]{1,19}/" + action))
        return "/admin/solicitacoes/{id}/" + action;
    for (String prefix : Set.of("/admin/cadastros/", "/admin/config/cadastros/"))
      for (String type : Set.of("espacos", "empreendedores", "visitas", "atendimentos",
          "personas", "precos", "feriados", "colaboradores", "configuracao")) {
        String base = prefix + type;
        for (String suffix : Set.of("", "/editar", "/salvar"))
          if (path.equals(base + suffix)) return base + suffix;
        if (path.matches(base + "/[0-9]{1,19}/excluir")) return base + "/{id}/excluir";
      }
    return "OTHER";
  }
}
