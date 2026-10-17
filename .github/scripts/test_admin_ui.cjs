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
