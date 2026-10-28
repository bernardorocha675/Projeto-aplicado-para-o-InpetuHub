// Executa o script real do menu do cabeçalho: botão do celular, submenus por botão e Esc.
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.resolve(__dirname, '../../aplicativo/src/main/resources/static/js/menu.js'), 'utf8');

function element(attrs = {}) {
  const listeners = {};
  const classes = new Set();
  return {
    hidden: true, dataset: {}, attrs: {...attrs}, listeners, focused: false,
    getAttribute(name) { return this.attrs[name] ?? null; },
    setAttribute(name, value) { this.attrs[name] = String(value); },
    addEventListener(name, fn) { listeners[name] = fn; },
    focus() { this.focused = true; },
    click() { listeners.click?.(); },
    classList: {toggle: (name, on) => { if (on) classes.add(name); else classes.delete(name); }, contains: name => classes.has(name)}
  };
}

function page({semCabecalho = false} = {}) {
  const botao = element({'aria-expanded': 'false', 'aria-controls': 'menu-principal'});
  const menu = element();
  const subBotoes = ['submenu-iniciativas', 'submenu-noticias'].map(id => element({'aria-expanded': 'false', 'aria-controls': id}));
  const listas = {'submenu-iniciativas': element(), 'submenu-noticias': element()};
  const topo = {
    dataset: {},
    querySelector: sel => {
      if (sel === '.menu-botao') return botao;
      if (sel === '.submenu-botao[aria-expanded="true"]') return subBotoes.find(b => b.getAttribute('aria-expanded') === 'true') || null;
      return null;
    },
    querySelectorAll: sel => (sel === '.submenu-botao' ? subBotoes : [])
  };
  const docListeners = {};
  vm.runInNewContext(source, {
    document: {
      querySelector: sel => (sel === '.topo' && !semCabecalho ? topo : null),
      getElementById: id => (id === 'menu-principal' ? menu : listas[id] || null),
      addEventListener: (name, fn) => { docListeners[name] = fn; }
    }
  });
  const tecla = key => docListeners.keydown?.({key});
  return {botao, topo, subBotoes, listas, tecla, docListeners};
}

test('com o script, o botão aparece e o menu começa fechado no celular', () => {
  const p = page();
  assert.equal(p.botao.hidden, false);
  assert.equal(p.topo.dataset.menu, 'fechado');
  assert.equal(p.botao.getAttribute('aria-expanded'), 'false');
  p.subBotoes.forEach(b => assert.equal(b.hidden, false));
});

test('o botão abre e fecha o menu e atualiza aria-expanded', () => {
  const p = page();
  p.botao.click();
  assert.equal(p.topo.dataset.menu, 'aberto');
  assert.equal(p.botao.getAttribute('aria-expanded'), 'true');
  p.botao.click();
  assert.equal(p.topo.dataset.menu, 'fechado');
  assert.equal(p.botao.getAttribute('aria-expanded'), 'false');
});

test('Esc fecha o menu aberto e devolve o foco ao botão', () => {
  const p = page();
  p.botao.click();
  p.tecla('Escape');
  assert.equal(p.topo.dataset.menu, 'fechado');
  assert.equal(p.botao.focused, true);
});

test('o botão do submenu abre a lista; Esc fecha primeiro o submenu e foca o botão dele', () => {
  const p = page();
  const [iniciativas] = p.subBotoes;
  iniciativas.click();
  assert.equal(iniciativas.getAttribute('aria-expanded'), 'true');
  assert.equal(p.listas['submenu-iniciativas'].classList.contains('aberto'), true);
  p.botao.click();
  p.tecla('Escape');
  assert.equal(iniciativas.getAttribute('aria-expanded'), 'false');
  assert.equal(p.listas['submenu-iniciativas'].classList.contains('aberto'), false);
  assert.equal(iniciativas.focused, true);
  assert.equal(p.topo.dataset.menu, 'aberto', 'o menu continua aberto até o segundo Esc');
  p.tecla('Escape');
  assert.equal(p.topo.dataset.menu, 'fechado');
});

test('abrir um submenu fecha o outro', () => {
  const p = page();
  const [iniciativas, noticias] = p.subBotoes;
  iniciativas.click();
  noticias.click();
  assert.equal(iniciativas.getAttribute('aria-expanded'), 'false');
  assert.equal(noticias.getAttribute('aria-expanded'), 'true');
});

test('outras teclas e páginas sem cabeçalho não fazem nada', () => {
  const p = page();
  p.botao.click();
  p.tecla('Enter');
  assert.equal(p.topo.dataset.menu, 'aberto');
  assert.doesNotThrow(() => page({semCabecalho: true}));
});
