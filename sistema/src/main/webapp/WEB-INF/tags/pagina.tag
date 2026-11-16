<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Layout de todas as telas" %>
<%@ attribute name="titulo" required="true" %>
<%@ attribute name="script" required="false" %>
<%@ attribute name="estilo" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="pt-BR">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width,initial-scale=1" />
    <title><c:out value="${titulo}" /> | InPETU Hub</title>
    <link rel="icon" href="/img/favicon.jpg" />
    <link rel="stylesheet" href="/css/tokens.css" />
    <link rel="stylesheet" href="/css/base.css" />
    <c:if test="${not empty estilo}"><link rel="stylesheet" href="${estilo}" /></c:if>
    <c:if test="${not empty script}"><script src="${script}" defer></script></c:if>
  </head>
  <body>
    <a class="pular" href="#conteudo">Pular para o conteúdo</a>
    <header class="topo">
      <a class="logo" href="/"><img src="/img/logo.png" alt="InPETU hub" width="200" height="33" /></a>
      <nav class="menu" aria-label="Principal">
        <a href="/agenda"<c:if test="${paginaAtual == 'agenda'}"> aria-current="page"</c:if>>Agenda</a>
        <a href="/admin"<c:if test="${paginaAtual == 'interna'}"> aria-current="page"</c:if>>Área Interna</a>
      </nav>
    </header>
    <main id="conteudo" class="principal">
      <jsp:doBody />
    </main>
    <footer class="rodape">
      <p>InPETU Hub · UFSC</p>
    </footer>
  </body>
</html>
