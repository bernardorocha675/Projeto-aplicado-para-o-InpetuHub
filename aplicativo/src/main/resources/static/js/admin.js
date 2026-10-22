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
// Telefone com país: o Brasil usa a máscara com DDD, como (48) 99808-2043; os outros países
// recebem só números e o servidor confere e formata pelo país escolhido.
const formatPhone = (value, region = 'BR') => {
  if (value.trimStart().startsWith('+') || region !== 'BR')
    return (value.trimStart().startsWith('+') ? '+' : '') + value.replace(/[^0-9 ().-]/g, '').slice(0, 19);
  const digits = value.replace(/\D/g, '').slice(0, 11);
  if (digits.length <= 2) return digits.length ? `(${digits}` : '';
  const local = digits.slice(2);
  const split = local.length > 8 ? 5 : 4;
  return local.length > split
    ? `(${digits.slice(0, 2)}) ${local.slice(0, split)}-${local.slice(split)}`
    : `(${digits.slice(0, 2)}) ${local}`;
};
const phoneCountry = input => (input.form ? input.form.querySelector('select[name=telefonePais]') : null);
// +351 escolhe Portugal: o maior código (1 a 3 dígitos) que corresponde a um país principal.
const countryForDial = (select, value) => {
  const digits = value.replace(/\D/g, '');
  for (let size = 3; size >= 1; size--) {
    const option = Array.from(select.options).find(o => o.dataset.principal === 'true' && o.dataset.dial === digits.slice(0, size));
    if (option && digits.length >= size) return option.value;
  }
  return null;
};
document.querySelectorAll('input[type=tel], input[name=telefone]').forEach(input => {
  const country = phoneCountry(input);
  const region = () => (country ? country.value : 'BR');
  input.setAttribute('inputmode', 'tel');
  input.maxLength = 20;
  input.addEventListener('input', () => {
    if (country && input.value.trimStart().startsWith('+')) {
      const found = countryForDial(country, input.value);
      if (found) country.value = found;
    }
    input.value = formatPhone(input.value, region());
  });
  if (country) country.addEventListener('change', () => { input.value = formatPhone(input.value, region()); });
  // Cadastros antigos com código do país sem + (mais de 11 dígitos) ficam como estão até serem editados.
  if (input.value.trimStart().startsWith('+') || region() !== 'BR' || input.value.replace(/\D/g, '').length <= 11)
    input.value = formatPhone(input.value, region());
});
// Cidade: sugestões do mundo todo, buscadas pelo servidor local. Sem resposta, o texto livre continua valendo.
// Escolher uma cidade marca o país do telefone, se o telefone ainda estiver vazio.
document.querySelectorAll('input[name=cidade]').forEach((input, index) => {
  const list = document.createElement('datalist');
  list.id = `cidades-sugeridas-${index}`;
  input.after(list);
  input.setAttribute('list', list.id);
  input.setAttribute('autocomplete', 'off');
  let timer, last = '', found = [];
  const phone = input.form ? input.form.querySelector('input[name=telefone]') : null;
  const country = phone ? phoneCountry(phone) : null;
  input.addEventListener('input', () => {
    const chosen = found.find(city => city.nome === input.value);
    if (chosen && country && phone && !phone.value.trim() && Array.from(country.options).some(o => o.value === chosen.pais))
      country.value = chosen.pais;
    clearTimeout(timer);
    const term = input.value.trim();
    if (term.length < 2 || term === last || term.includes(',')) return;
    timer = setTimeout(() => {
      last = term;
      fetch(`/cidades?q=${encodeURIComponent(term)}`, {headers: {Accept: 'application/json'}})
        .then(response => (response.ok ? response.json() : []))
        .then(cities => {
          if (input.value.trim() !== term || !Array.isArray(cities)) return;
          found = cities;
          list.replaceChildren(...cities.map(city => Object.assign(document.createElement('option'), {value: city.nome})));
        })
        .catch(() => {});
    }, 300);
  });
});
// Documento: o tipo define o que o número aceita; "Sem documento" esvazia e desativa o número.
// O servidor confere de novo (Documentos.normalizar).
const documentRules = {
  CNPJ: {max: 14, clean: value => value.replace(/\D/g, '')},
  CPF: {max: 11, clean: value => value.replace(/\D/g, '')},
  ESTRANGEIRO: {max: 30, clean: value => value.toUpperCase().replace(/[^A-Z0-9./-]/g, '')}
};
document.querySelectorAll('select[name=tipoDocumento]').forEach(type => {
  const number = type.form ? type.form.querySelector('input[name=cnpj]') : null;
  if (!number) return;
  const rule = () => documentRules[type.value];
  const apply = () => {
    const r = rule();
    number.disabled = !r;
    number.required = Boolean(r);
    if (!r) number.value = '';
    else {
      number.maxLength = r.max;
      number.value = r.clean(number.value);
    }
  };
  number.addEventListener('input', () => {
    const r = rule();
    if (r) number.value = r.clean(number.value).slice(0, r.max);
  });
  type.addEventListener('change', apply);
  apply();
});
