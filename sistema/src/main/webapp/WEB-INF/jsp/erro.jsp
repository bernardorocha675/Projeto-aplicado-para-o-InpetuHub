<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pagina titulo="${titulo}">
  <section class="cartao">
    <h1><c:out value="${titulo}" /></h1>
    <p class="alerta erro" role="alert"><c:out value="${mensagem}" /></p>
    <a class="botao" href="/">Voltar ao início</a>
    <c:if test="${not empty referencia}">
      <p class="discreto">Referência para suporte: <c:out value="${referencia}" /></p>
    </c:if>
  </section>
</t:pagina>
