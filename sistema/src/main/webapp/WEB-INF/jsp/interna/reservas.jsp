<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:interna titulo="Reservas">
  <p class="sobretitulo">Gestão da agenda</p>
  <h1>Reservas</h1>
  <section class="cartao">
    <form method="get" action="/admin/reservas" class="filtros-lista">
      <div class="campo"><label for="f-status">Situação</label>
        <select id="f-status" name="status"><option value="">Todas</option><c:forEach var="o" items="${situacoes}"><option value="${o.valor()}"<c:if test="${o.selecionado()}"> selected</c:if>><c:out value="${o.rotulo()}" /></option></c:forEach></select></div>
      <div class="campo"><label for="f-data">Data</label><input id="f-data" name="data" type="date" value="<c:out value='${filtroData}' />" /></div>
      <div class="campo"><label for="f-espaco">Espaço</label>
        <select id="f-espaco" name="espaco"><option value="">Todos</option><c:forEach var="o" items="${espacos}"><option value="${o.valor()}"<c:if test="${o.selecionado()}"> selected</c:if>><c:out value="${o.rotulo()}" /></option></c:forEach></select></div>
      <div class="campo"><label for="f-persona">Persona</label>
        <select id="f-persona" name="persona"><option value="">Todas</option><c:forEach var="o" items="${personas}"><option value="${o.valor()}"<c:if test="${o.selecionado()}"> selected</c:if>><c:out value="${o.rotulo()}" /></option></c:forEach></select></div>
      <div class="campo"><label for="f-busca">Solicitante</label><input id="f-busca" name="busca" value="<c:out value='${busca}' />" placeholder="Nome, empresa ou documento…" /></div>
      <div class="acoes"><button type="submit" class="botao">Filtrar</button><a class="botao secundario" href="/admin/reservas">Limpar</a></div>
    </form>
    <t:tabelaReservas linhas="${linhas}" />
  </section>
</t:interna>
