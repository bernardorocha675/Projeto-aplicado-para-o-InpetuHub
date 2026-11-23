<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pagina titulo="Revisar solicitação" estilo="/css/pedido.css">
  <div class="faixa-pagina">
    <p class="sobretitulo">InPETU Hub</p>
    <h1>Confira sua solicitação</h1>
    <p>O envio registra uma solicitação para a equipe conferir. Ele não bloqueia a agenda.</p>
  </div>
  <div class="duas-colunas">
    <section class="cartao-oficial">
      <h2>Dados da reserva</h2>
      <dl class="dados">
        <c:forEach var="l" items="${linhas}"><dt><c:out value="${l.key}" /></dt><dd><c:out value="${l.value}" /></dd></c:forEach>
      </dl>
    </section>
    <section class="cartao-oficial">
      <h2>Estimativa</h2>
      <c:if test="${not taxasConfiguradas}">
        <p class="alerta aviso" role="status">As taxas de operação e limpeza ainda não foram configuradas. A estimativa usa os valores disponíveis e será conferida antes da aprovação.</p>
      </c:if>
      <dl class="dados">
        <c:forEach var="l" items="${valores}"><dt><c:out value="${l.key}" /></dt><dd><c:out value="${l.value}" /></dd></c:forEach>
      </dl>
      <p class="discreto">A equipe confere de novo o horário, a cota e os valores na aprovação.</p>
      <form method="post" action="/reservas/solicitar" class="acoes">
        <input type="hidden" name="_csrf" value="${csrf}" />
        <button type="submit" class="botao-grande">Confirmar solicitação</button>
        <a class="botao secundario" href="/reserva?editar=true">Voltar para editar</a>
      </form>
    </section>
  </div>
</t:pagina>
