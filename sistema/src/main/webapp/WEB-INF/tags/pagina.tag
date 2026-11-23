<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Layout de todas as telas: cabeçalho e rodapé do site oficial" %>
<%@ attribute name="titulo" required="true" %>
<%@ attribute name="script" required="false" %>
<%@ attribute name="estilo" required="false" %>
<%@ attribute name="lateral" required="false" fragment="true" %>
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
    <c:if test="${not empty lateral}"><link rel="stylesheet" href="/css/interna.css" /></c:if>
    <c:if test="${not empty estilo}"><link rel="stylesheet" href="${estilo}" /></c:if>
    <script src="/js/tela.js" defer></script>
    <c:if test="${not empty script}"><script src="${script}" defer></script></c:if>
  </head>
  <body>
    <a class="pular" href="#conteudo">Pular para o conteúdo</a>
    <header class="topo">
      <a class="logo" href="/"><img src="/img/logo.png" alt="InPETU hub" width="200" height="33" /></a>
      <nav class="menu" aria-label="Principal">
        <a href="/">Início</a>
        <a href="https://inpetuhub.sites.ufsc.br/aa_rasunho_sobre/" target="_blank" rel="noopener">Sobre</a>
        <a href="https://inpetuhub.sites.ufsc.br/acoes-mobilizadoras/" target="_blank" rel="noopener">Iniciativas</a>
        <a href="https://inpetuhub.sites.ufsc.br/category/eventos/" target="_blank" rel="noopener">Notícias e eventos</a>
        <a href="/agenda"<c:if test="${paginaAtual == 'agenda'}"> aria-current="page"</c:if>>Agenda</a>
        <a href="https://inpetuhub.sites.ufsc.br/contato/" target="_blank" rel="noopener">Contato</a>
        <a href="/admin"<c:if test="${paginaAtual == 'interna'}"> aria-current="page"</c:if>>Área Interna</a>
      </nav>
    </header>
    <div class="${empty lateral ? 'sem-lateral' : 'com-lateral'}">
      <c:if test="${not empty lateral}"><jsp:invoke fragment="lateral" /></c:if>
      <main id="conteudo" class="principal">
        <jsp:doBody />
      </main>
    </div>
    <footer class="rodape">
      <div class="rodape-dados">
        <p class="rodape-nome">InPETU hub</p>
        <p>Sapiens Parque – Florianópolis/SC – Brasil</p>
        <p>E-mail: <a href="mailto:contato@inpetu.com.br">contato@inpetu.com.br</a> | Telefone: <a href="tel:+554832392030">+55 (48) 3239-2030</a></p>
        <p>© 2025 InPETU Hub. Todos os direitos reservados.</p>
        <p><a href="https://inpetuhub.sites.ufsc.br/politica-de-privacidade" target="_blank" rel="noopener">Política de Privacidade</a> | Termos de Uso</p>
        <p class="rodape-projeto">InPETU Hub CRM · Agenda e conexões · Projeto aplicado UniSENAI</p>
      </div>
      <div class="rodape-redes">
        <a href="https://www.facebook.com/InPETUhub?locale=pt_BR" target="_blank" rel="noopener" aria-label="Facebook"><svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M14 8h3V4h-3c-2.8 0-4.5 1.8-4.5 4.6V11H7v4h2.5v8h4v-8h3l.5-4h-3.5V8.8c0-.5.3-.8.5-.8z" /></svg></a>
        <a href="https://www.instagram.com/inpetuhub/" target="_blank" rel="noopener" aria-label="Instagram"><svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M12 7.3A4.7 4.7 0 1 0 12 16.7 4.7 4.7 0 0 0 12 7.3zm0 7.7a3 3 0 1 1 0-6 3 3 0 0 1 0 6zm4.9-8.9a1.1 1.1 0 1 0 0 2.2 1.1 1.1 0 0 0 0-2.2zM20.9 7c-.1-1.5-.4-2.9-1.5-4S16.9 1.6 15.4 1.5C13.9 1.4 10.1 1.4 8.6 1.5 7.1 1.6 5.7 1.9 4.6 3S3.2 5.5 3.1 7C3 8.5 3 12.3 3.1 13.8c.1 1.5.4 2.9 1.5 4s2.5 1.4 4 1.5c1.5.1 5.3.1 6.8 0 1.5-.1 2.9-.4 4-1.5s1.4-2.5 1.5-4c.1-1.5.1-5.3 0-6.8zm-2 8.4a2.8 2.8 0 0 1-1.6 1.6c-1.1.4-3.7.3-4.9.3s-3.8.1-4.9-.3a2.8 2.8 0 0 1-1.6-1.6c-.4-1.1-.3-3.7-.3-4.9s-.1-3.8.3-4.9A2.8 2.8 0 0 1 7.5 4c1.1-.4 3.7-.3 4.9-.3s3.8-.1 4.9.3a2.8 2.8 0 0 1 1.6 1.6c.4 1.1.3 3.7.3 4.9s.1 3.8-.3 4.9z" /></svg></a>
        <a href="https://www.youtube.com/@InPETUhub" target="_blank" rel="noopener" aria-label="YouTube"><svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M22.5 7.2a2.8 2.8 0 0 0-2-2C18.8 4.8 12 4.8 12 4.8s-6.8 0-8.5.4a2.8 2.8 0 0 0-2 2C1.1 8.9 1.1 12 1.1 12s0 3.1.4 4.8a2.8 2.8 0 0 0 2 2c1.7.4 8.5.4 8.5.4s6.8 0 8.5-.4a2.8 2.8 0 0 0 2-2c.4-1.7.4-4.8.4-4.8s0-3.1-.4-4.8zM9.8 15.2V8.8l5.7 3.2-5.7 3.2z" /></svg></a>
      </div>
    </footer>
  </body>
</html>
