// Pede confirmação antes de enviar um formulário que não tem volta (excluir, recusar, cancelar).
// O texto vem do atributo data-confirmar. Sem JavaScript o formulário continua funcionando.
document.addEventListener('submit', (evento) => {
  const formulario = evento.target;
  if (!(formulario instanceof HTMLFormElement) || !formulario.hasAttribute('data-confirmar')) return;
  const aceitou = window.confirm(formulario.getAttribute('data-confirmar'));
  window.avisarTela?.('CLIQUE', aceitou ? 'CONFIRMAR_SIM' : 'CONFIRMAR_NAO');
  if (!aceitou) evento.preventDefault();
});
