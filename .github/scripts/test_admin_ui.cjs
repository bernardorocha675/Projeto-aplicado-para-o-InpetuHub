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

test('telefone recusa letras, formata o DDD e limita a 11 dígitos', () => {
  const p = phone();
  assert.equal(p.input.maxLength, 15);
  assert.equal(p.input.attributes.inputmode, 'numeric');
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
