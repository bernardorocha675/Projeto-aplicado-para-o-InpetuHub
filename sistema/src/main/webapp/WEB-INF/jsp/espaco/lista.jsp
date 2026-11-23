<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:interna titulo="Espaços">
  <section class="cartao">
    <p class="sobretitulo">Área Interna</p>
    <h1>Espaços</h1>
    <c:if test="${not empty aviso}">
      <p class="alerta sucesso" role="status"><c:out value="${aviso}" /></p>
    </c:if>
    <div class="acoes">
      <a class="botao" href="/admin/cadastros/espacos/editar">Cadastrar espaço</a>
      <a class="botao secundario" href="/admin">Voltar à Área Interna</a>
    </div>
    <div class="rolagem-tabela">
      <table class="tabela">
        <caption class="discreto">Espaços em ordem de nome. Excluir só é possível sem reservas; para tirar da agenda, desmarque “Ativo” ou “Disponível para reservas”.</caption>
        <thead>
          <tr>
            <th scope="col">Nome</th>
            <th scope="col">Capacidade</th>
            <th scope="col">Mobiliário</th>
            <th scope="col">Disponível para reservas</th>
            <th scope="col">Ativo</th>
            <th scope="col">Ações</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="e" items="${espacos}">
            <tr>
              <th scope="row"><c:out value="${e.nome()}" /></th>
              <td>${e.capacidade()}</td>
              <td><c:out value="${empty e.mobiliario() ? '—' : e.mobiliario()}" /></td>
              <td>${e.disponivel() ? 'Sim' : 'Não'}</td>
              <td>${e.ativo() ? 'Sim' : 'Não'}</td>
              <td>
                <div class="acoes-linha">
                  <a class="botao secundario" href="/admin/cadastros/espacos/editar?id=${e.id()}">Editar<span class="so-leitor"> <c:out value="${e.nome()}" /></span></a>
                <form method="post" action="/admin/cadastros/espacos/excluir">
                  <input type="hidden" name="_csrf" value="${csrf}" />
                  <input type="hidden" name="id" value="${e.id()}" />
                  <button type="submit" class="botao secundario">Excluir<span class="so-leitor"> <c:out value="${e.nome()}" /></span></button>
                </form>
                </div>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </section>
</t:interna>
