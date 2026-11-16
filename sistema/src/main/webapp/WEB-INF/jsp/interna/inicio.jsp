<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pagina titulo="Área Interna">
  <section class="cartao">
    <p class="sobretitulo">Área Interna</p>
    <h1>Olá, <c:out value="${nomeDaConta}" /></h1>
    <p>Perfil: <c:out value="${perfilDaConta}" /></p>
    <nav aria-label="Área Interna">
      <ul class="lista-links">
        <li><a href="/admin/cadastros/espacos">Espaços</a></li>
        <li><a href="/agenda">Agenda pública</a></li>
      </ul>
    </nav>
    <div class="acoes">
      <a class="botao secundario" href="/conta/senha">Alterar senha</a>
      <form method="post" action="/sair">
        <input type="hidden" name="_csrf" value="${csrf}" />
        <button type="submit" class="botao">Sair</button>
      </form>
    </div>
  </section>
</t:pagina>
