// Executa o script administrativo real para conferir o reenvio após correção/Voltar.
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.resolve(__dirname, '../../aplicativo/src/main/resources/static/js/admin.js'), 'utf8');

function page(valid = true) {
  const button = {disabled: false};
  const listeners = {}, windowListeners = {};
  const form = {
    method: 'post', dataset: {repeatSubmit: 'true'}, checkValidity: () => valid,
    addEventListener: (name, fn) => {listeners[name] = fn;},
    querySelectorAll: () => [button]
  };
  vm.runInNewContext(source, {
    document: {querySelectorAll: selector => selector === 'form' ? [form] : []},
    window: {addEventListener: (name, fn) => {windowListeners[name] = fn;}},
    setTimeout: fn => fn()
  });
  return {button, listeners, windowListeners};
}

test('cadastro de espaços permite salvar de novo após Voltar pelo cache', () => {
  const p = page();
  p.listeners.submit();
  assert.equal(p.button.disabled, true);
  p.windowListeners.pageshow({persisted: true});
  assert.equal(p.button.disabled, false);
  p.listeners.submit();
  assert.equal(p.button.disabled, true);
});

test('validação do navegador não bloqueia o botão e nova resposta começa habilitada', () => {
  const p = page(false);
  p.listeners.submit();
  assert.equal(p.button.disabled, false);
  assert.equal(page().button.disabled, false);
});

function phone(initial = '') {
  const listeners = {};
  const input = {value: initial, attributes: {}, setAttribute(name, value) { this.attributes[name] = value; },
    addEventListener: (name, fn) => {listeners[name] = fn;}};
  vm.runInNewContext(source, {
    document: {querySelectorAll: selector => selector.includes('tel') ? [input] : []},
    window: {addEventListener: () => {}}, setTimeout: fn => fn()
  });
  const type = value => { input.value = value; listeners.input(); return input.value; };
  return {input, type};
}

test('telefone do Brasil recusa letras, formata o DDD e limita a 11 dígitos', () => {
  const p = phone();
  assert.equal(p.input.maxLength, 20);
  assert.equal(p.input.attributes.inputmode, 'tel');
  assert.equal(p.type('gfhfghdgdg'), '');
  assert.equal(p.type('4'), '(4');
  assert.equal(p.type('489980'), '(48) 9980');
  assert.equal(p.type('48998082043'), '(48) 99808-2043');
  assert.equal(p.type('(48) 99808-20439999'), '(48) 99808-2043');
  assert.equal(p.type('4833334444'), '(48) 3333-4444');
});

test('telefone salvo é exibido formatado, sem cortar números antigos com código do país', () => {
  assert.equal(phone('48999990000').input.value, '(48) 99999-0000');
  assert.equal(phone('+55 48 99999-0000').input.value, '+55 48 99999-0000');
});

test('telefone de outro país começa com + e não recebe a máscara do Brasil', () => {
  const p = phone();
  assert.equal(p.type('+351 912 345 678'), '+351 912 345 678');
  assert.equal(p.type('+1 (415) 555-0100abc'), '+1 (415) 555-0100');
  assert.equal(p.type('+44 20 7946 0958 9999 9999'), '+44 20 7946 0958 999');
  assert.equal(phone('+351912345678').input.value, '+351912345678');
});

function city(responses) {
  const listeners = {}, requests = [], timers = [];
  const input = {value: '', attributes: {}, setAttribute(name, value) { this.attributes[name] = value; },
    addEventListener: (name, fn) => {listeners[name] = fn;}, after(node) { this.sibling = node; }};
  const document = {
    querySelectorAll: selector => selector.includes('cidade') ? [input] : [],
    createElement: tag => ({tag, children: [], replaceChildren(...nodes) { this.children = nodes; }})
  };
  vm.runInNewContext(source, {
    document, window: {addEventListener: () => {}},
    setTimeout: fn => { timers.push(fn); return timers.length; }, clearTimeout: () => {},
    fetch: url => { requests.push(url); const r = responses.shift(); return r instanceof Error ? Promise.reject(r) : Promise.resolve({ok: true, json: () => r}); }
  });
  const type = async value => { input.value = value; listeners.input(); timers.splice(0).forEach(fn => fn()); await new Promise(r => setImmediate(r)); };
  return {input, requests, type};
}

test('cidade busca sugestões do mundo pelo servidor local e aceita texto livre sem resposta', async () => {
  const c = city([[{nome: 'Lisboa, Lisboa, Portugal', pais: 'PT'}, {nome: 'Lisboa, Paraíba, Brasil', pais: 'BR'}], new Error('offline')]);
  assert.equal(c.input.attributes.list, c.input.sibling.id);
  await c.type('L');
  assert.equal(c.requests.length, 0);
  await c.type('Lisb');
  assert.equal(c.requests[0], '/cidades?q=Lisb');
  assert.deepEqual(c.input.sibling.children.map(o => o.value), ['Lisboa, Lisboa, Portugal', 'Lisboa, Paraíba, Brasil']);
  await c.type('Lisboa, Lisboa, Portugal');
  assert.equal(c.requests.length, 1);
  await c.type('São José');
  assert.equal(c.requests[1], '/cidades?q=S%C3%A3o%20Jos%C3%A9');
  assert.equal(c.input.value, 'São José');
});

function phoneForm(region = 'BR', phoneValue = '') {
  const listeners = {}, countryListeners = {}, cityListeners = {}, timers = [];
  const options = [['BR', '55', 'true'], ['PT', '351', 'true'], ['US', '1', 'true'], ['CA', '1', 'false'], ['AO', '244', 'true']]
    .map(([value, dial, principal]) => ({value, dataset: {dial, principal}}));
  const country = {value: region, options, addEventListener: (name, fn) => { countryListeners[name] = fn; }};
  const form = {querySelector: selector => selector.includes('telefonePais') ? country : selector.includes('telefone') ? input : null};
  const input = {value: phoneValue, form, attributes: {}, setAttribute(n, v) { this.attributes[n] = v; },
    addEventListener: (name, fn) => {listeners[name] = fn;}};
  const cityInput = {value: '', form, attributes: {}, setAttribute(n, v) { this.attributes[n] = v; },
    addEventListener: (name, fn) => {cityListeners[name] = fn;}, after() {}};
  vm.runInNewContext(source, {
    document: {
      querySelectorAll: s => s.includes('tel') ? [input] : s.includes('cidade') ? [cityInput] : [],
      createElement: () => ({replaceChildren() {}})
    },
    window: {addEventListener: () => {}},
    setTimeout: fn => { timers.push(fn); return 1; }, clearTimeout: () => {},
    fetch: () => Promise.resolve({ok: true, json: () => [{nome: 'Lisboa, Distrito de Lisboa, Portugal', pais: 'PT'}]})
  });
  const type = value => { input.value = value; listeners.input(); return input.value; };
  const typeCity = async value => { cityInput.value = value; cityListeners.input(); timers.splice(0).forEach(fn => fn()); await new Promise(r => setImmediate(r)); };
  return {input, country, countryListeners, type, typeCity};
}

test('digitar + e o código do país escolhe o país no seletor', () => {
  const p = phoneForm();
  assert.equal(p.type('+351 912 345 678'), '+351 912 345 678');
  assert.equal(p.country.value, 'PT');
  p.type('+1 650 253 0000');
  assert.equal(p.country.value, 'US');
  p.type('+244 923 000 000');
  assert.equal(p.country.value, 'AO');
});

test('país fora do Brasil não recebe a máscara de DDD e trocar o país reformata', () => {
  const p = phoneForm('PT');
  assert.equal(p.type('912 345 678'), '912 345 678');
  p.country.value = 'BR';
  p.countryListeners.change();
  assert.equal(p.input.value, '(91) 2345-678');
});

test('escolher a cidade marca o país do telefone só se o telefone estiver vazio', async () => {
  const p = phoneForm();
  await p.typeCity('Lisb');
  await p.typeCity('Lisboa, Distrito de Lisboa, Portugal');
  assert.equal(p.country.value, 'PT');
  const q = phoneForm('BR', '(48) 99808-2043');
  await q.typeCity('Lisb');
  await q.typeCity('Lisboa, Distrito de Lisboa, Portugal');
  assert.equal(q.country.value, 'BR');
});
