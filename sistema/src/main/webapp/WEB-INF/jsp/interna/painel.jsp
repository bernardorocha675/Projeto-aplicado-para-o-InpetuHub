<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:interna titulo="Painel">
  <p class="sobretitulo">Visão geral</p>
  <h1>Painel</h1>
  <p>Olá, <c:out value="${nomeDaConta}" />. Reservas e pedidos em um só lugar.</p>
  <div class="numeros">
    <section class="cartao numero">
      <h2>Solicitações recebidas</h2>
      <p class="valor">${pedidosPendentes}</p>
      <a href="/admin/solicitacoes">Analisar solicitações</a>
    </section>
    <section class="cartao numero">
      <h2>Reservas pendentes</h2>
      <p class="valor">${reservasPendentes}</p>
      <a href="/admin/reservas?status=PENDENTE">Ver pendentes</a>
    </section>
    <section class="cartao numero">
      <h2>Reservas de hoje</h2>
      <p class="valor">${reservasHoje}</p>
      <a href="/agenda">Abrir agenda pública</a>
    </section>
  </div>
  <section class="cartao">
    <h2>Próximas reservas</h2>
    <t:tabelaReservas linhas="${proximas}" />
  </section>
</t:interna>
