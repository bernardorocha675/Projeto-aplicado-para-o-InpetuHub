// Pede confirmação antes de enviar um formulário que não tem volta (excluir, recusar, cancelar).
// O texto vem do atributo data-confirmar. Sem JavaScript o formulário continua funcionando.
document.addEventListener('submit', (evento) => {
  const formulario = evento.target;
  if (!(formulario instanceof HTMLFormElement) || !formulario.hasAttribute('data-confirmar')) return;
  if (!window.confirm(formulario.getAttribute('data-confirmar'))) evento.preventDefault();
});
