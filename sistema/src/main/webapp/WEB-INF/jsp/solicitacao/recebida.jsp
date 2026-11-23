<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pagina titulo="${enviado ? 'Solicitação recebida' : 'Consultar solicitação'}" estilo="/css/pedido.css">
  <div class="faixa-pagina">
    <p class="sobretitulo">InPETU Hub</p>
    <h1>${enviado ? 'Solicitação recebida' : 'Consultar solicitação'}</h1>
  </div>
  <section class="cartao-oficial">
    <c:choose>
      <c:when test="${enviado}">
        <p>A solicitação não bloqueia a agenda. A equipe do Hub fará a conferência manual do contato e do vínculo com a instituição e, depois, aprovará ou recusará a reserva.</p>
        <p>Aguarde o contato da equipe; não é preciso procurar nenhum código.</p>
      </c:when>
      <c:otherwise>
        <p>Nenhuma solicitação foi enviada neste navegador. Consulte a agenda e preencha o formulário de reserva.</p>
      </c:otherwise>
    </c:choose>
    <a class="botao-grande" href="/agenda">Voltar à agenda</a>
  </section>
</t:pagina>
