<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:interna titulo="Reserva #${id}">
  <p class="sobretitulo">Reserva #${id}</p>
  <h1><c:out value="${titulo}" /></h1>
  <p><span class="situacao ${classeSituacao}"><c:out value="${situacao}" /></span> <a href="/admin/reservas">Voltar à lista</a></p>
  <c:if test="${not empty aviso}"><p class="alerta sucesso" role="status"><c:out value="${aviso}" /></p></c:if>
  <div class="duas-colunas-interna">
    <section class="cartao">
      <h2>Solicitante e evento</h2>
      <dl class="dados"><c:forEach var="l" items="${solicitante}"><dt><c:out value="${l.key}" /></dt><dd><c:out value="${l.value}" /></dd></c:forEach></dl>
    </section>
    <section class="cartao">
      <h2>Cota e valores</h2>
      <dl class="dados"><c:forEach var="l" items="${valores}"><dt><c:out value="${l.key}" /></dt><dd><c:out value="${l.value}" /></dd></c:forEach></dl>
      <c:if test="${not taxasConfiguradas}"><p class="alerta aviso" role="status">Estimativa preliminar: as taxas de operação e limpeza ainda não foram configuradas.</p></c:if>
      <c:if test="${not empty motivoRecusa}"><p class="alerta erro" role="alert">Motivo da recusa: <c:out value="${motivoRecusa}" /></p></c:if>
    </section>
  </div>

  <c:if test="${podeAnalisar}">
    <section class="cartao">
      <h2>Análise</h2>
      <p>Confira a persona declarada e os dados do evento antes de aprovar. Horário, cota e valores são conferidos de novo na aprovação.</p>
      <form method="post" action="/admin/reservas/${id}/aprovar">
        <input type="hidden" name="_csrf" value="${csrf}" />
        <button type="submit" class="botao">Aprovar reserva</button>
      </form>
      <form method="post" action="/admin/reservas/${id}/recusar" class="formulario">
        <input type="hidden" name="_csrf" value="${csrf}" />
        <label for="motivo">Motivo da recusa *</label>
        <textarea id="motivo" name="motivo" maxlength="2000" required></textarea>
        <button type="submit" class="botao secundario">Recusar e liberar o horário</button>
      </form>
    </section>
  </c:if>

  <c:if test="${podeAlterar}">
    <section class="cartao">
      <h2>Alterar reserva</h2>
      <form method="post" action="/admin/reservas/${id}/alterar" class="formulario">
        <input type="hidden" name="_csrf" value="${csrf}" />
        <div class="grade-interna">
          <div class="campo"><label for="a-data">Data *</label><input id="a-data" name="data" type="date" value="${dataValor}" required /></div>
          <div class="campo"><label for="a-espaco">Espaço *</label><select id="a-espaco" name="espacoId"><c:forEach var="o" items="${espacos}"><option value="${o.valor()}"<c:if test="${o.selecionado()}"> selected</c:if>><c:out value="${o.rotulo()}" /></option></c:forEach></select></div>
          <div class="campo"><label for="a-inicio">Hora inicial *</label><input id="a-inicio" name="horaInicio" type="time" value="${inicioValor}" required /></div>
          <div class="campo"><label for="a-fim">Hora final *</label><input id="a-fim" name="horaFim" type="time" value="${fimValor}" required /></div>
          <div class="campo"><label for="a-persona">Persona conferida *</label><select id="a-persona" name="personaId"><c:forEach var="o" items="${personas}"><option value="${o.valor()}"<c:if test="${o.selecionado()}"> selected</c:if>><c:out value="${o.rotulo()}" /></option></c:forEach></select></div>
        </div>
        <button type="submit" class="botao">Salvar e recalcular</button>
      </form>
      <h3>Cancelar reserva</h3>
      <form method="post" action="/admin/reservas/${id}/cancelar" class="formulario">
        <input type="hidden" name="_csrf" value="${csrf}" />
        <p class="alerta aviso" role="status">Com menos de 30 dias de antecedência, cancelar uma reserva aprovada que usou cota gera penalidade de 2× as horas.</p>
        <c:if test="${gestor}"><label class="caixa"><input type="checkbox" name="forcaMaior" value="sim" /> Força maior / exceção do gestor (com justificativa)</label></c:if>
        <label for="justificativa">Justificativa</label>
        <textarea id="justificativa" name="justificativa" maxlength="2000"></textarea>
        <button type="submit" class="botao secundario">Cancelar reserva</button>
      </form>
    </section>
  </c:if>

  <c:if test="${podePagar}">
    <section class="cartao">
      <h2>Registro financeiro</h2>
      <p class="discreto">Registro manual. O sistema não cobra nem processa pagamentos.</p>
      <form method="post" action="/admin/reservas/${id}/pagamento" class="formulario">
        <input type="hidden" name="_csrf" value="${csrf}" />
        <label for="pagamento">Pagamento</label>
        <select id="pagamento" name="situacao"><c:forEach var="o" items="${pagamentos}"><option value="${o.valor()}"<c:if test="${o.selecionado()}"> selected</c:if>><c:out value="${o.rotulo()}" /></option></c:forEach></select>
        <button type="submit" class="botao">Registrar pagamento</button>
      </form>
      <c:if test="${podeConcluir}">
        <form method="post" action="/admin/reservas/${id}/concluir">
          <input type="hidden" name="_csrf" value="${csrf}" />
          <button type="submit" class="botao secundario">Marcar como concluída (depois do evento)</button>
        </form>
      </c:if>
    </section>
  </c:if>

  <section class="cartao">
    <h2>Histórico</h2>
    <ol class="historico">
      <c:forEach var="h" items="${historico}">
        <li><strong><c:out value="${h[0]}" /></strong> <span class="discreto"><c:out value="${h[1]}" /></span><p><c:out value="${h[2]}" /></p></li>
      </c:forEach>
    </ol>
  </section>
</t:interna>
