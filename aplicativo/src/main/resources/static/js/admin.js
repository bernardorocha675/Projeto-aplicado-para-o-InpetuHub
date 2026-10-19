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
// Telefone: somente números, com DDD entre parênteses, como (48) 99808-2043.
const formatPhone = value => {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  if (digits.length <= 2) return digits.length ? `(${digits}` : '';
  const local = digits.slice(2);
  const split = local.length > 8 ? 5 : 4;
  return local.length > split
    ? `(${digits.slice(0, 2)}) ${local.slice(0, split)}-${local.slice(split)}`
    : `(${digits.slice(0, 2)}) ${local}`;
};
document.querySelectorAll('input[type=tel], input[name=telefone]').forEach(input => {
  input.setAttribute('inputmode', 'numeric');
  input.maxLength = 15;
  input.addEventListener('input', () => {
    input.value = formatPhone(input.value);
  });
  // Cadastros antigos com código do país (mais de 11 dígitos) ficam como estão até serem editados.
  if (input.value.replace(/\D/g, '').length <= 11) input.value = formatPhone(input.value);
});
