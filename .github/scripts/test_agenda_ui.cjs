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

class CalendarDate extends Date { constructor(...args) { super(...(args.length ? args : ['2026-10-18T12:00:00Z'])); } }

const source = fs.readFileSync(path.resolve(__dirname, '../../aplicativo/src/main/resources/static/js/agenda.js'), 'utf8');
async function calendar(state, rooms = []) {
  const nodes = Object.fromEntries(['calendario', 'mes-titulo', 'detalhe-dia', 'filtro-espaco', 'agenda-aviso', 'mes-anterior', 'mes-proximo', 'filtro-dia'].map(id => [id, new Element()]));
  let current = state;
  const context = {
    document: {getElementById: id => nodes[id], createElement: tag => new Element(tag)},
    URLSearchParams, Date: CalendarDate, setInterval: () => {},
    fetch: async url => {
      const month = new URLSearchParams(url.split('?')[1]).get('mes');
      return {ok: true, json: async () => ({dias: [{data: month + '-20', estado: current, feriado: false, espacos: rooms}]})};
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
  ['PARCIAL', '◐ Parcial', 'partial'],
  ['INDISPONÍVEL', '● Ocupado', 'full']
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

function flattened(node) { return [node, ...node.children.flatMap(flattened)]; }

test('seleção de dia mostra nomes, capacidades, ocupação e links com data e espaço', async () => {
 const page=await calendar('PARCIAL',[{id:7,nome:'Sala São José',capacidade:25,
   ocupados:[{inicio:'08:00',fim:'09:00',titulo:'Reunião'}],livres:[{inicio:'09:00',fim:'18:00'}]}]);
 page.day().listeners.click();
 const nodes=flattened(page.nodes['detalhe-dia']);
 assert.ok(nodes.some(n=>n.textContent==='Sala São José · até 25 pessoas'));
 assert.ok(nodes.some(n=>n.textContent==='08:00–09:00 · Reunião'));
 const link=nodes.find(n=>n.tag==='a');const params=new URLSearchParams(link.href.split('?')[1]);
 assert.equal(params.get('data'),page.nodes['filtro-dia'].value);assert.equal(params.get('espacoId'),'7');
 assert.equal(params.get('horaInicio'),'09:00');assert.equal(params.get('horaFim'),'18:00');
});

test('consulta por dia muda o mês, mantém a seleção ao filtrar e limita navegação aos dois anos', async () => {
 const page=await calendar('DISPONÍVEL');const year=2026;
 const picker=page.nodes['filtro-dia'];picker.value=`${year+1}-12-20`;picker.listeners.change();await page.flush();
 assert.equal(page.nodes['mes-proximo'].disabled,true);assert.equal(page.day().attributes['aria-pressed'],'true');
 await page.nodes['filtro-espaco'].listeners.change();await page.flush();assert.equal(picker.value,`${year+1}-12-20`);
 picker.value=`${year}-01-20`;picker.listeners.change();await page.flush();assert.equal(page.nodes['mes-anterior'].disabled,true);
});
