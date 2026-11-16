<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pagina titulo="Solicitar reserva" estilo="/css/pedido.css">
  <div class="faixa-pagina">
    <p class="sobretitulo">InPETU Hub</p>
    <h1>Solicitar uma reserva</h1>
    <p>Informe os dados para consultar o orçamento antes de confirmar.</p>
  </div>
  <form method="post" action="/reservas/resumo" class="cartao-oficial form-oficial">
    <input type="hidden" name="_csrf" value="${csrf}" />
    <p class="discreto">Campos com * são obrigatórios.</p>
    <c:if test="${not empty erroGeral or not empty erros}">
      <div class="alerta erro" role="alert">
        <c:if test="${not empty erroGeral}"><p><c:out value="${erroGeral}" /></p></c:if>
        <c:if test="${not empty erros}">
          <p><strong>Confira os campos:</strong></p>
          <c:forEach var="e" items="${erros}">
            <p><a href="#${e.key}"><c:out value="${rotulos[e.key]}" />: <c:out value="${e.value}" /></a></p>
          </c:forEach>
        </c:if>
      </div>
    </c:if>

    <h2>Espaço e horário</h2>
    <div class="grade">
      <t:campo nome="data" rotulo="Data" tipo="date" valor="${v.data}" erro="${erros.data}" obrigatorio="true" min="${hoje}" max="${ultimoDia}" />
      <div class="campo">
        <label for="espacoId">Espaço *</label>
        <select id="espacoId" name="espacoId" required<c:if test="${not empty erros.espacoId}"> aria-invalid="true" aria-describedby="erro-espacoId"</c:if>>
          <option value="">Selecione</option>
          <c:forEach var="o" items="${espacos}"><option value="${o.valor()}"<c:if test="${o.selecionado()}"> selected</c:if>><c:out value="${o.rotulo()}" /></option></c:forEach>
        </select>
        <c:if test="${not empty erros.espacoId}"><p id="erro-espacoId" class="erro-campo"><c:out value="${erros.espacoId}" /></p></c:if>
      </div>
      <t:campo nome="horaInicio" rotulo="Hora inicial" tipo="time" valor="${v.horaInicio}" erro="${erros.horaInicio}" obrigatorio="true" />
      <t:campo nome="horaFim" rotulo="Hora final" tipo="time" valor="${v.horaFim}" erro="${erros.horaFim}" obrigatorio="true" />
    </div>

    <h2>Dados do solicitante</h2>
    <div class="grade">
      <t:campo nome="nome" rotulo="Nome" valor="${v.nome}" erro="${erros.nome}" obrigatorio="true" maxlength="255" autocomplete="name" />
      <div class="campo">
        <label for="tipoDocumento">Documento *</label>
        <select id="tipoDocumento" name="tipoDocumento" required<c:if test="${not empty erros.tipoDocumento}"> aria-invalid="true" aria-describedby="erro-tipoDocumento"</c:if>>
          <c:forEach var="o" items="${tiposDocumento}"><option value="${o.valor()}"<c:if test="${o.selecionado()}"> selected</c:if>><c:out value="${o.rotulo()}" /></option></c:forEach>
        </select>
        <c:if test="${not empty erros.tipoDocumento}"><p id="erro-tipoDocumento" class="erro-campo"><c:out value="${erros.tipoDocumento}" /></p></c:if>
      </div>
      <t:campo nome="documento" rotulo="Número do documento (em branco com “Sem documento”)" valor="${v.documento}" erro="${erros.documento}" maxlength="30" autocomplete="off" />
      <t:campo nome="empresa" rotulo="Empresa/instituição" valor="${v.empresa}" erro="${erros.empresa}" maxlength="255" autocomplete="organization" />
      <t:campo nome="email" rotulo="Email" tipo="email" valor="${v.email}" erro="${erros.email}" obrigatorio="true" maxlength="255" autocomplete="email" />
      <t:campo nome="telefone" rotulo="Telefone (no Brasil, com DDD; de outro país, com + e o código)" tipo="tel" valor="${v.telefone}" erro="${erros.telefone}" obrigatorio="true" maxlength="20" autocomplete="tel" inputmode="tel" />
      <t:campo nome="cidade" rotulo="Cidade" valor="${v.cidade}" erro="${erros.cidade}" obrigatorio="true" maxlength="255" autocomplete="address-level2" />
      <t:campo nome="segmento" rotulo="Segmento" valor="${v.segmento}" erro="${erros.segmento}" maxlength="255" />
      <t:campo nome="cargo" rotulo="Cargo" valor="${v.cargo}" erro="${erros.cargo}" maxlength="255" autocomplete="organization-title" />
    </div>

    <h2>Evento</h2>
    <div class="grade">
      <t:campo nome="titulo" rotulo="Nome do evento" valor="${v.titulo}" erro="${erros.titulo}" obrigatorio="true" maxlength="255" />
      <div class="campo">
        <label for="personaId">Persona declarada *</label>
        <select id="personaId" name="personaId" required<c:if test="${not empty erros.personaId}"> aria-invalid="true" aria-describedby="erro-personaId"</c:if>>
          <option value="">Selecione</option>
          <c:forEach var="o" items="${personas}"><option value="${o.valor()}"<c:if test="${o.selecionado()}"> selected</c:if>><c:out value="${o.rotulo()}" /></option></c:forEach>
        </select>
        <c:if test="${not empty erros.personaId}"><p id="erro-personaId" class="erro-campo"><c:out value="${erros.personaId}" /></p></c:if>
      </div>
      <t:campo nome="numeroParticipantes" rotulo="Quantidade de participantes" tipo="number" valor="${v.numeroParticipantes}" erro="${erros.numeroParticipantes}" obrigatorio="true" min="1" step="1" />
    </div>
    <div class="campo">
      <label for="objetivo">Objetivo *</label>
      <textarea id="objetivo" name="objetivo" maxlength="2000" required<c:if test="${not empty erros.objetivo}"> aria-invalid="true" aria-describedby="erro-objetivo"</c:if>><c:out value="${v.objetivo}" /></textarea>
      <c:if test="${not empty erros.objetivo}"><p id="erro-objetivo" class="erro-campo"><c:out value="${erros.objetivo}" /></p></c:if>
    </div>
    <div class="campo">
      <label for="observacoes">Observações</label>
      <textarea id="observacoes" name="observacoes" maxlength="2000"<c:if test="${not empty erros.observacoes}"> aria-invalid="true" aria-describedby="erro-observacoes"</c:if>><c:out value="${v.observacoes}" /></textarea>
      <c:if test="${not empty erros.observacoes}"><p id="erro-observacoes" class="erro-campo"><c:out value="${erros.observacoes}" /></p></c:if>
    </div>
    <div class="caixas">
      <label><input type="checkbox" name="inscricaoPaga"<c:if test="${f.inscricaoPaga}"> checked</c:if> /> O evento possui inscrição paga</label>
      <label><input type="checkbox" name="financiado"<c:if test="${f.financiado}"> checked</c:if> /> O evento possui patrocínio/financiamento</label>
      <label><input type="checkbox" name="academico"<c:if test="${f.academico}"> checked</c:if> /> É uma atividade acadêmica (P2)</label>
    </div>
    <p class="discreto">A persona será conferida pela equipe. O envio não bloqueia a agenda: a equipe confere o contato e aprova a reserva.</p>
    <div class="acoes">
      <button type="submit" class="botao-grande">Calcular e revisar solicitação</button>
      <a class="botao secundario" href="/agenda">Voltar à agenda</a>
    </div>
  </form>
</t:pagina>
