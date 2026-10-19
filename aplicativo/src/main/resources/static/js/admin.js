document.querySelectorAll('.voltar').forEach(b => b.addEventListener('click', () => history.back()));
document.querySelectorAll('.confirm-delete').forEach(f =>
  f.addEventListener('submit', e => {
    if (!confirm('Excluir este registro? Cadastros vinculados a reservas devem ser desativados.')) e.preventDefault();
  })
);
document.querySelectorAll('.sidebar nav a').forEach(a => {
  const current =
    location.pathname === new URL(a.href).pathname ||
    (a.pathname != '/admin' && location.pathname.startsWith(a.pathname));
  if (current) a.classList.add('current');
});
document.querySelectorAll('form').forEach(form => {
  if (form.method.toLowerCase() !== 'post') return;
  const buttons = () => form.querySelectorAll('button[type=submit],button:not([type])');
  form.addEventListener('submit', () => {
    if (!form.checkValidity()) return;
    setTimeout(() => buttons().forEach(button => { button.disabled = true; }), 0);
  });
  // Voltar pode restaurar a página pelo cache com o botão ainda desabilitado.
  if (form.dataset.repeatSubmit === 'true') {
    window.addEventListener('pageshow', () => buttons().forEach(button => { button.disabled = false; }));
  }
});
