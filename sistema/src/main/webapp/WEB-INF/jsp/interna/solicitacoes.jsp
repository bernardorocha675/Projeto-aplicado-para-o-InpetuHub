<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:interna titulo="Solicitações recebidas">
  <p class="sobretitulo">Pedidos públicos</p>
  <h1>Solicitações recebidas</h1>
  <c:if test="${not empty aviso}"><p class="alerta sucesso" role="status"><c:out value="${aviso}" /></p></c:if>
  <p>Os dados abaixo são declarações do solicitante. Conferir documento e contato não prova a representação da instituição: confirme com um responsável conhecido antes de encaminhar. O pedido não bloqueia a agenda.</p>
  <c:if test="${empty cartoes}"><p class="cartao">Nenhuma solicitação aguardando análise.</p></c:if>
  <c:forEach var="s" items="${cartoes}">
    <article class="cartao pedido">
      <h2><c:out value="${s.titulo()}" /></h2>
      <dl class="dados">
        <dt>Solicitante</dt><dd><c:out value="${s.quem()}" /></dd>
        <dt>Contato</dt><dd><c:out value="${s.contato()}" /></dd>
        <dt>Data e horário</dt><dd><c:out value="${s.quando()}" /></dd>
        <dt>Espaço</dt><dd><c:out value="${s.espaco()}" /></dd>
        <dt>Persona declarada</dt><dd><c:out value="${s.persona()}" /></dd>
        <dt>Objetivo</dt><dd><c:out value="${s.objetivo()}" /></dd>
        <dt>Vence em</dt><dd><c:out value="${s.expira()}" /></dd>
      </dl>
      <form method="post" action="/admin/solicitacoes/aceitar" class="formulario">
        <input type="hidden" name="_csrf" value="${csrf}" />
        <input type="hidden" name="id" value="${s.id()}" />
        <label class="caixa"><input type="checkbox" name="identidade" value="sim" required /> Conferi a identidade e o vínculo com a instituição por um contato confiável *</label>
        <label for="como-${s.id()}">Como foi conferido *</label>
        <textarea id="como-${s.id()}" name="como" maxlength="2000" required></textarea>
        <button type="submit" class="botao">Encaminhar para análise da reserva</button>
      </form>
      <form method="post" action="/admin/solicitacoes/recusar">
        <input type="hidden" name="_csrf" value="${csrf}" />
        <input type="hidden" name="id" value="${s.id()}" />
        <button type="submit" class="botao secundario">Recusar<span class="so-leitor"> o pedido <c:out value="${s.titulo()}" /></span></button>
      </form>
    </article>
  </c:forEach>
</t:interna>
