<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pagina titulo="Entrar" script="/js/mostrar-senha.js">
  <section class="cartao estreito">
    <p class="sobretitulo">Área Interna</p>
    <h1>Bem-vindo ao Hub</h1>
    <p>Acesse a gestão de reservas e relacionamentos.</p>
    <c:if test="${not empty aviso}">
      <p class="alerta ${tipoAviso}" role="${tipoAviso == 'erro' ? 'alert' : 'status'}"><c:out value="${aviso}" /></p>
    </c:if>
    <form method="post" action="/login" class="formulario">
      <input type="hidden" name="_csrf" value="${csrf}" />
      <label for="usuario">Usuário</label>
      <input id="usuario" name="usuario" type="text" autocomplete="username" autocapitalize="none" spellcheck="false" maxlength="255" required />
      <label for="senha">Senha</label>
      <input id="senha" name="senha" type="password" autocomplete="current-password" autocapitalize="none" spellcheck="false" maxlength="1024" required />
      <button type="submit" class="botao">Entrar</button>
    </form>
  </section>
</t:pagina>
