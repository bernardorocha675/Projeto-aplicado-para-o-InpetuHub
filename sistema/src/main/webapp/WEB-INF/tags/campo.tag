<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" dynamic-attributes="extras" description="Campo de formulário com rótulo e erro ligado" %>
<%@ attribute name="nome" required="true" %>
<%@ attribute name="rotulo" required="true" %>
<%@ attribute name="tipo" required="false" %>
<%@ attribute name="valor" required="false" %>
<%@ attribute name="erro" required="false" %>
<%@ attribute name="obrigatorio" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<div class="campo">
  <label for="${nome}">${fn:escapeXml(rotulo)}<c:if test="${obrigatorio}"> *</c:if></label>
  <input id="${nome}" name="${nome}" type="${empty tipo ? 'text' : tipo}" value="${fn:escapeXml(valor)}"<c:if test="${obrigatorio}"> required</c:if><c:forEach var="a" items="${extras}">${' '}${a.key}="${fn:escapeXml(a.value)}"</c:forEach><c:if test="${not empty erro}"> aria-invalid="true" aria-describedby="erro-${nome}"</c:if> />
  <c:if test="${not empty erro}"><p id="erro-${nome}" class="erro-campo">${fn:escapeXml(erro)}</p></c:if>
</div>
