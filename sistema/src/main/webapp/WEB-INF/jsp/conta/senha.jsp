<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pagina titulo="Alterar senha" script="/js/mostrar-senha.js">
  <section class="cartao estreito">
    <h1>Alterar senha</h1>
    <p>A alteração encerra todas as suas sessões. Use de 8 a 64 caracteres.</p>
    <c:if test="${modoLocal}">
      <p class="alerta aviso" role="status">Nesta demonstração local, as senhas das contas de demonstração voltam ao padrão quando o sistema reinicia.</p>
    </c:if>
    <c:if test="${not empty erro}">
      <p class="alerta erro" role="alert"><c:out value="${erro}" /></p>
    </c:if>
    <form method="post" action="/conta/senha" class="formulario">
      <input type="hidden" name="_csrf" value="${csrf}" />
      <label for="atual">Senha atual</label>
      <input id="atual" name="atual" type="password" autocomplete="current-password" maxlength="1024" required />
      <label for="nova">Nova senha</label>
      <input id="nova" name="nova" type="password" autocomplete="new-password" minlength="8" maxlength="64" required />
      <button type="submit" class="botao">Alterar e sair</button>
    </form>
    <a class="botao secundario" href="/admin">Voltar sem alterar</a>
  </section>
</t:pagina>
