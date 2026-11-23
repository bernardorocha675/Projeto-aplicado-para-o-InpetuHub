<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Layout da Área Interna: menu lateral e conteúdo" %>
<%@ attribute name="titulo" required="true" %>
<%@ attribute name="estilo" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pagina titulo="${titulo}" estilo="${estilo}">
  <jsp:attribute name="lateral">
    <aside class="lateral">
      <p class="sobretitulo">Gestão do Hub</p>
      <nav class="menu-interno" aria-label="Menu interno">
        <a href="/admin"<c:if test="${menuAtual == 'painel'}"> aria-current="page"</c:if>>Painel</a>
        <a href="/admin/reservas"<c:if test="${menuAtual == 'reservas'}"> aria-current="page"</c:if>>Reservas</a>
        <a href="/admin/solicitacoes"<c:if test="${menuAtual == 'solicitacoes'}"> aria-current="page"</c:if>>Solicitações recebidas<c:if test="${pedidosPendentes > 0}"><span class="ponto-pendente" aria-hidden="true"></span><span class="so-leitor"> (há solicitações aguardando análise)</span></c:if></a>
        <a href="/admin/cadastros/espacos"<c:if test="${menuAtual == 'espacos'}"> aria-current="page"</c:if>>Espaços</a>
        <a href="/agenda">Agenda pública</a>
        <a href="/conta/senha">Alterar senha</a>
      </nav>
      <form method="post" action="/sair">
        <input type="hidden" name="_csrf" value="${csrf}" />
        <button type="submit" class="botao secundario">Sair</button>
      </form>
    </aside>
  </jsp:attribute>
  <jsp:body><jsp:doBody /></jsp:body>
</t:pagina>
