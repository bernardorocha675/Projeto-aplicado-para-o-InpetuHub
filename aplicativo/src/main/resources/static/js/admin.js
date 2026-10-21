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
// Telefone do Brasil: somente números, com DDD entre parênteses, como (48) 99808-2043.
// Outros países: começa com + e o código do país, sem máscara, como +351 912 345 678.
const formatPhone = value => {
  if (value.trimStart().startsWith('+')) return '+' + value.replace(/[^0-9 ().-]/g, '').slice(0, 19);
  const digits = value.replace(/\D/g, '').slice(0, 11);
  if (digits.length <= 2) return digits.length ? `(${digits}` : '';
  const local = digits.slice(2);
  const split = local.length > 8 ? 5 : 4;
  return local.length > split
    ? `(${digits.slice(0, 2)}) ${local.slice(0, split)}-${local.slice(split)}`
    : `(${digits.slice(0, 2)}) ${local}`;
};
document.querySelectorAll('input[type=tel], input[name=telefone]').forEach(input => {
  input.setAttribute('inputmode', 'tel');
  input.maxLength = 20;
  input.addEventListener('input', () => {
    input.value = formatPhone(input.value);
  });
  // Cadastros antigos com código do país sem + (mais de 11 dígitos) ficam como estão até serem editados.
  if (input.value.trimStart().startsWith('+') || input.value.replace(/\D/g, '').length <= 11) input.value = formatPhone(input.value);
});
// Cidade: sugestões do mundo todo, buscadas pelo servidor local. Sem resposta, o texto livre continua valendo.
document.querySelectorAll('input[name=cidade]').forEach((input, index) => {
  const list = document.createElement('datalist');
  list.id = `cidades-sugeridas-${index}`;
  input.after(list);
  input.setAttribute('list', list.id);
  input.setAttribute('autocomplete', 'off');
  let timer, last = '';
  input.addEventListener('input', () => {
    clearTimeout(timer);
    const term = input.value.trim();
    if (term.length < 2 || term === last || term.includes(',')) return;
    timer = setTimeout(() => {
      last = term;
      fetch(`/cidades?q=${encodeURIComponent(term)}`, {headers: {Accept: 'application/json'}})
        .then(response => (response.ok ? response.json() : []))
        .then(names => {
          if (input.value.trim() !== term || !Array.isArray(names)) return;
          list.replaceChildren(...names.map(name => Object.assign(document.createElement('option'), {value: name})));
        })
        .catch(() => {});
    }, 300);
  });
});
