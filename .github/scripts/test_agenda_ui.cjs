// Executa o JavaScript real da agenda sem dependências de navegador ou de npm.
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

class Element {
  constructor(tag = 'div') {
    this.tag = tag;
    this.children = [];
    this.textContent = '';
    this.className = '';
    this.value = '';
    this.hidden = true;
    this.attributes = {};
    this.listeners = {};
    this.classList = {
      add: value => { this.className += ' ' + value; },
      remove: value => { this.className = this.className.split(' ').filter(c => c !== value).join(' '); },
      contains: value => this.className.split(' ').includes(value)
    };
  }
  append(...children) { this.children.push(...children); }
  replaceChildren(...children) { this.children = children; }
  setAttribute(name, value) { this.attributes[name] = value; }
  addEventListener(name, listener) { this.listeners[name] = listener; }
  querySelectorAll(selector) { return this.children.filter(c => c.classList.contains(selector.slice(1))); }
}

const source = fs.readFileSync(path.resolve(__dirname, '../../aplicativo/src/main/resources/static/js/agenda.js'), 'utf8');
async function calendar(state) {
  const nodes = Object.fromEntries(['calendario', 'mes-titulo', 'detalhe-dia', 'filtro-espaco', 'agenda-aviso', 'mes-anterior', 'mes-proximo'].map(id => [id, new Element()]));
  let current = state;
  const context = {
    document: {getElementById: id => nodes[id], createElement: tag => new Element(tag)},
    URLSearchParams, Date, setInterval: () => {},
    fetch: async url => {
      const month = new URLSearchParams(url.split('?')[1]).get('mes');
      return {ok: true, json: async () => ({dias: [{data: month + '-20', estado: current, feriado: false, espacos: []}]})};
    }
  };
  vm.runInNewContext(source, context);
  const flush = () => new Promise(setImmediate);
  await flush();
  return {nodes, flush, setState: value => {current = value;}, day: () => nodes.calendario.children.find(n => !n.disabled)};
}

test('banco sem espaços não exibe Ocupado nem Livre e explica como habilitar a agenda', async () => {
  const page = await calendar('SEM_ESPACOS');
  const day = page.day();
  assert.equal(day.children[1].textContent, '— Sem espaços');
  assert.equal(day.classList.contains('full'), false);
  assert.equal(day.classList.contains('empty-day'), true);
  assert.equal(page.nodes['agenda-aviso'].hidden, false);
  assert.match(day.attributes['aria-label'], /Sem espaços habilitados/);
  day.listeners.click();
  assert.ok(page.nodes['detalhe-dia'].children.some(n => /Não há espaços habilitados/.test(n.textContent)));
});

for (const [state, label, style] of [
  ['DISPONÍVEL', '○ Livre', null],
  ['PARCIAL', '● Parcial', 'partial'],
  ['INDISPONÍVEL', '× Ocupado', 'full']
]) {
  test('preserva a apresentação de ' + state, async () => {
    const page = await calendar(state);
    assert.equal(page.day().children[1].textContent, label);
    if (style) assert.equal(page.day().classList.contains(style), true);
    assert.equal(page.day().classList.contains('empty-day'), false);
    assert.equal(page.nodes['agenda-aviso'].hidden, true);
  });
}

test('ao atualizar a consulta após cadastrar um espaço, remove o aviso e mostra Livre', async () => {
  const page = await calendar('SEM_ESPACOS');
  page.setState('DISPONÍVEL');
  await page.nodes['filtro-espaco'].listeners.change();
  await page.flush();
  assert.equal(page.nodes['agenda-aviso'].hidden, true);
  assert.equal(page.day().children[1].textContent, '○ Livre');
});
