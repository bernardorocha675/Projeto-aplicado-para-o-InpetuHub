<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Tabela de reservas" %>
<%@ attribute name="linhas" required="true" type="java.util.List" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<div class="rolagem-tabela">
  <table class="tabela">
    <thead>
      <tr>
        <th scope="col">Data e horário</th>
        <th scope="col">Solicitante / empresa</th>
        <th scope="col">Espaço</th>
        <th scope="col">Persona</th>
        <th scope="col">Situação</th>
        <th scope="col">Ações</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="l" items="${linhas}">
        <tr>
          <td><c:out value="${l.dia()}" /> · <c:out value="${l.horario()}" /></td>
          <th scope="row"><c:out value="${l.solicitante()}" /><br /><span class="discreto"><c:out value="${l.empresa()}" /></span></th>
          <td><c:out value="${l.espaco()}" /></td>
          <td><c:out value="${l.persona()}" /></td>
          <td><span class="situacao ${l.classe()}"><c:out value="${l.situacao()}" /></span></td>
          <td><a class="botao secundario" href="/admin/reservas/${l.id()}">Analisar<span class="so-leitor"> a reserva de <c:out value="${l.solicitante()}" /></span></a></td>
        </tr>
      </c:forEach>
      <c:if test="${empty linhas}"><tr><td colspan="6">Nenhuma reserva encontrada.</td></tr></c:if>
    </tbody>
  </table>
</div>
