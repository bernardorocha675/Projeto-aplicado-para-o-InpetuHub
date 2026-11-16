<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="f" value="${formulario}" />
<t:pagina titulo="${empty f.id ? 'Cadastrar espaço' : 'Editar espaço'}">
  <section class="cartao estreito">
    <p class="sobretitulo">Espaços</p>
    <h1>${empty f.id ? 'Cadastrar espaço' : 'Editar espaço'}</h1>
    <c:if test="${not empty f.erros}">
      <div class="alerta erro" role="alert">
        <p><strong>Confira os campos:</strong></p>
        <c:forEach var="erro" items="${f.erros}">
          <p><a href="#${erro.key}"><c:out value="${erro.value}" /></a></p>
        </c:forEach>
      </div>
    </c:if>
    <form method="post" action="/admin/cadastros/espacos/salvar" class="formulario">
      <input type="hidden" name="_csrf" value="${csrf}" />
      <input type="hidden" name="id" value="<c:out value='${f.id}' />" />
      <label for="nome">Nome do espaço</label>
      <input id="nome" name="nome" type="text" maxlength="255" required value="<c:out value='${f.nome}' />"<c:if test="${not empty f.erros.nome}"> aria-invalid="true" aria-describedby="erro-nome"</c:if> />
      <c:if test="${not empty f.erros.nome}"><p id="erro-nome" class="erro-campo"><c:out value="${f.erros.nome}" /></p></c:if>
      <label for="capacidade">Capacidade (cadeiras)</label>
      <input id="capacidade" name="capacidade" type="number" step="1" min="1" max="2147483647" inputmode="numeric" required value="<c:out value='${f.capacidade}' />"<c:if test="${not empty f.erros.capacidade}"> aria-invalid="true" aria-describedby="erro-capacidade"</c:if> />
      <c:if test="${not empty f.erros.capacidade}"><p id="erro-capacidade" class="erro-campo"><c:out value="${f.erros.capacidade}" /></p></c:if>
      <label for="mobiliario">Mobiliário (opcional, ex.: 32 cadeiras e 6 sofás)</label>
      <input id="mobiliario" name="mobiliario" type="text" maxlength="255" value="<c:out value='${f.mobiliario}' />"<c:if test="${not empty f.erros.mobiliario}"> aria-invalid="true" aria-describedby="erro-mobiliario"</c:if> />
      <c:if test="${not empty f.erros.mobiliario}"><p id="erro-mobiliario" class="erro-campo"><c:out value="${f.erros.mobiliario}" /></p></c:if>
      <label class="caixa"><input name="disponivel" type="checkbox"<c:if test="${f.disponivel}"> checked</c:if> /> Disponível para reservas</label>
      <label class="caixa"><input name="ativo" type="checkbox"<c:if test="${f.ativo}"> checked</c:if> /> Ativo</label>
      <button type="submit" class="botao">Salvar</button>
    </form>
    <a class="botao secundario" href="/admin/cadastros/espacos">Voltar sem salvar</a>
  </section>
</t:pagina>
