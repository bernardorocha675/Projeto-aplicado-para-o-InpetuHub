<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pagina titulo="Agenda" estilo="/css/agenda.css" script="/js/agenda.js">
  <section class="agenda">
    <div class="agenda-faixa">
      <p class="sobretitulo">InPETU Hub</p>
      <h1>Agenda de Eventos, Visitações e Conexões</h1>
      <p>Encontre um espaço para sua próxima conexão. Consulte a disponibilidade e envie sua solicitação de reserva.</p>
      <p class="nota">As solicitações são analisadas pela equipe do Hub.</p>
    </div>
    <div class="agenda-calendario">
      <div class="mes">
        <button type="button" id="mes-anterior" class="seta" aria-label="Mês anterior">‹</button>
        <h2 id="mes-titulo" aria-live="polite"></h2>
        <button type="button" id="mes-proximo" class="seta" aria-label="Próximo mês">›</button>
      </div>
      <div class="filtros">
        <div class="filtro">
          <label for="filtro-espaco">Consultar espaço</label>
          <select id="filtro-espaco">
            <option value="">Todos os espaços</option>
            <c:forEach var="e" items="${espacos}">
              <option value="${e.id()}"><c:out value="${e.nome()}" /> · <c:out value="${e.descricao()}" /></option>
            </c:forEach>
          </select>
        </div>
        <div class="filtro">
          <label for="filtro-dia">Consultar dia</label>
          <input id="filtro-dia" type="date" min="${primeiroDia}" max="${ultimoDia}" />
        </div>
      </div>
      <c:if test="${equipeLogada}">
        <div id="agenda-aviso" class="alerta aviso" role="status" hidden>
          <strong>Nenhum espaço cadastrado para reservas.</strong>
          Os dias aparecem como “Sem espaços” até a equipe do Hub cadastrar um espaço ativo e disponível para reservas
          (ou reativar um existente).
          <a class="botao" href="/admin/cadastros/espacos/editar">Cadastrar espaço</a>
          <a href="/admin/cadastros/espacos">Ver espaços cadastrados</a>
        </div>
      </c:if>
      <div class="semana" aria-hidden="true">
        <span>Dom</span><span>Seg</span><span>Ter</span><span>Qua</span><span>Qui</span><span>Sex</span><span>Sáb</span>
      </div>
      <div id="calendario" class="calendario" data-hoje="${hoje}" data-primeiro-dia="${primeiroDia}" data-ultimo-dia="${ultimoDia}" role="group" aria-label="Calendário de disponibilidade"></div>
      <p class="dica">Clique em um dia marcado para ver os eventos.</p>
      <div id="detalhe-dia" aria-live="polite" class="detalhe-dia">
        <p>Selecione uma data para consultar os espaços, capacidades e horários.</p>
      </div>
    </div>
  </section>
</t:pagina>
