'use strict';
// Agenda pública (porta do agenda.js do antigo): monta o calendário do mês a partir de /disponibilidade.
// Dia com atividade marcada fica azul com um ponto; os horários aparecem ao clicar no dia.
(() => {
  const grade = document.getElementById('calendario');
  const titulo = document.getElementById('mes-titulo');
  const detalhe = document.getElementById('detalhe-dia');
  const filtro = document.getElementById('filtro-espaco');
  const aviso = document.getElementById('agenda-aviso');
  const escolhaDia = document.getElementById('filtro-dia');
  const anterior = document.getElementById('mes-anterior');
  const proximo = document.getElementById('mes-proximo');
  if (!grade) {
    return;
  }

  const iso = (d) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  const hoje = grade.dataset.hoje || iso(new Date());
  const primeiroMes = (grade.dataset.primeiroDia || hoje.slice(0, 4) + '-01-01').slice(0, 7);
  const ultimoDia = grade.dataset.ultimoDia || `${Number(hoje.slice(0, 4)) + 1}-12-31`;
  const ultimoMes = ultimoDia.slice(0, 7);
  let mes = new Date(hoje + 'T12:00:00');
  mes.setDate(1);
  let escolhido = null;
  let pedido = 0;

  const el = (tag, texto, classe) => {
    const n = document.createElement(tag);
    if (texto) n.textContent = texto;
    if (classe) n.className = classe;
    return n;
  };
  const semDados = () =>
    detalhe.replaceChildren(el('p', 'Selecione uma data para consultar os espaços, capacidades e horários.'));

  // automatica === true só na atualização periódica: sem cookie, ela não renova a sessão de quem
  // está logado (o tempo sem uso continua valendo). A resposta é a mesma para todos.
  async function mostrarMes(automatica) {
    const numero = ++pedido;
    const exibido = iso(mes).slice(0, 7);
    anterior.disabled = exibido <= primeiroMes;
    proximo.disabled = exibido >= ultimoMes;
    if (escolhaDia) escolhaDia.value = escolhido || '';
    const nome = mes.toLocaleDateString('pt-BR', { month: 'long' });
    titulo.textContent = `${nome.charAt(0).toUpperCase()}${nome.slice(1)} ${mes.getFullYear()}`;
    if (automatica !== true) grade.replaceChildren(el('p', 'Carregando…', 'mensagem'));
    try {
      const consulta = new URLSearchParams({ mes: exibido });
      if (filtro.value) consulta.set('espacoId', filtro.value);
      const resposta = await fetch('/disponibilidade?' + consulta, automatica === true ? { credentials: 'omit' } : undefined);
      if (!resposta.ok) throw new Error('Não foi possível consultar a agenda.');
      const json = await resposta.json();
      if (numero !== pedido) return;
      const dias = new Map(json.dias.map((d) => [d.data, d]));
      // O aviso de cadastro de espaços só existe na página de quem está logado.
      if (aviso) aviso.hidden = !json.dias.length || !json.dias.every((d) => d.estado === 'SEM_ESPACOS');
      grade.replaceChildren();
      const inicio = new Date(mes);
      inicio.setDate(1 - inicio.getDay());
      for (let i = 0; i < 42; i++) {
        const d = new Date(inicio);
        d.setDate(inicio.getDate() + i);
        if (i >= 35 && d.getMonth() !== mes.getMonth()) break;
        grade.append(botaoDia(d, dias.get(iso(d))));
      }
      if (escolhido && dias.has(escolhido)) mostrarDia(dias.get(escolhido));
      else semDados();
    } catch (erro) {
      if (numero === pedido) {
        // Sem dados atuais, os horários e links da consulta anterior não podem continuar visíveis.
        detalhe.replaceChildren(el('p', 'A agenda não pôde ser atualizada. Tente consultar o mês novamente.'));
        grade.replaceChildren(el('p', erro.message, 'mensagem alerta erro'));
      }
    }
  }

  function botaoDia(d, info) {
    const data = iso(d);
    const botao = el('button', null, 'dia');
    botao.type = 'button';
    botao.append(el('strong', String(d.getDate())));
    if (!info) {
      botao.classList.add('fora');
      botao.disabled = true;
      return botao;
    }
    const eventos = info.espacos.reduce((total, sala) => total + sala.ocupados.length, 0);
    if (eventos) {
      botao.classList.add('com-atividade');
      const ponto = el('span', '•', 'ponto');
      ponto.setAttribute('aria-hidden', 'true');
      botao.append(ponto);
    }
    if (data === hoje) botao.classList.add('hoje');
    if (data === escolhido) botao.classList.add('escolhido');
    const atividade = eventos
      ? `${eventos} ${eventos === 1 ? 'atividade marcada' : 'atividades marcadas'}`
      : 'sem atividades marcadas';
    const situacao =
      info.estado === 'SEM_ESPACOS' ? ', sem espaços habilitados' : info.estado === 'INDISPONÍVEL' ? ', sem horários livres' : '';
    botao.setAttribute('aria-label', `${d.toLocaleDateString('pt-BR')}: ${atividade}${situacao}${info.feriado ? ', feriado' : ''}`);
    botao.setAttribute('aria-pressed', data === escolhido ? 'true' : 'false');
    botao.addEventListener('click', () => {
      escolhido = data;
      if (escolhaDia) escolhaDia.value = data;
      grade.querySelectorAll('.dia').forEach((outro) => {
        outro.classList.remove('escolhido');
        outro.setAttribute('aria-pressed', 'false');
      });
      botao.classList.add('escolhido');
      botao.setAttribute('aria-pressed', 'true');
      mostrarDia(info);
    });
    return botao;
  }

  function mostrarDia(info) {
    detalhe.replaceChildren();
    const extenso = new Date(info.data + 'T12:00:00').toLocaleDateString('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long'
    });
    detalhe.append(el('h3', extenso + (info.feriado ? ' · Feriado' : '')));
    if (info.data < hoje) {
      detalhe.append(el('p', 'Data passada: consulte os eventos. Novas reservas não são permitidas.', 'discreto'));
    }
    if (!info.espacos.length) {
      detalhe.append(el('p', 'Não há espaços habilitados para reservas nesta consulta. Isso não indica que os horários estejam ocupados.'));
    }
    for (const sala of info.espacos) detalhe.append(cartaoSala(info, sala));
  }

  function cartaoSala(info, sala) {
    const cartao = el('section', null, 'espaco-horarios');
    cartao.append(el('h4', sala.nome + (sala.mobiliario ? ` · ${sala.mobiliario}` : ` · até ${sala.capacidade} pessoas`)));
    if (sala.ocupados.length) {
      cartao.append(el('p', 'Horários indisponíveis', 'discreto'));
      for (const evento of sala.ocupados) cartao.append(el('p', `${evento.inicio}–${evento.fim} · ${evento.titulo}`, 'ocupado'));
    }
    cartao.append(el('p', sala.livres.length ? 'Horários disponíveis' : 'Sem horários livres', 'discreto'));
    const horarios = el('div', null, 'horarios');
    for (const livre of sala.livres) {
      if (info.data >= hoje && info.data <= ultimoDia) {
        const link = el('a', `${livre.inicio}–${livre.fim}`, 'horario');
        link.href = '/reserva?' + new URLSearchParams({ data: info.data, espacoId: sala.id, horaInicio: livre.inicio, horaFim: livre.fim });
        horarios.append(link);
      } else {
        horarios.append(el('span', `${livre.inicio}–${livre.fim}`, 'horario'));
      }
    }
    cartao.append(horarios);
    if (info.data >= hoje && info.data <= ultimoDia && sala.livres.length) cartao.append(formularioPedido(info, sala));
    return cartao;
  }

  function formularioPedido(info, sala) {
    const form = el('form', null, 'form-horario');
    form.action = '/reserva';
    form.method = 'get';
    for (const [nome, valor] of Object.entries({ data: info.data, espacoId: sala.id })) {
      const campo = el('input');
      campo.type = 'hidden';
      campo.name = nome;
      campo.value = valor;
      form.append(campo);
    }
    for (const [nome, texto, valor] of [
      ['horaInicio', 'Início', sala.livres[0].inicio],
      ['horaFim', 'Fim', sala.livres[0].fim]
    ]) {
      const rotulo = el('label', texto);
      const campo = el('input');
      campo.type = 'time';
      campo.name = nome;
      campo.value = valor;
      campo.required = true;
      rotulo.append(campo);
      form.append(rotulo);
    }
    const botao = el('button', 'Solicitar reserva', 'botao');
    botao.type = 'submit';
    form.append(botao);
    return form;
  }

  anterior.addEventListener('click', () => {
    if (iso(mes).slice(0, 7) <= primeiroMes) return;
    mes.setMonth(mes.getMonth() - 1);
    escolhido = null;
    mostrarMes();
  });
  proximo.addEventListener('click', () => {
    if (iso(mes).slice(0, 7) >= ultimoMes) return;
    mes.setMonth(mes.getMonth() + 1);
    escolhido = null;
    mostrarMes();
  });
  if (escolhaDia) {
    escolhaDia.addEventListener('change', () => {
      const valor = escolhaDia.value;
      if (!/^\d{4}-\d{2}-\d{2}$/.test(valor) || valor.slice(0, 7) < primeiroMes || valor > ultimoDia) return;
      escolhido = valor;
      mes = new Date(valor + 'T12:00:00');
      mes.setDate(1);
      mostrarMes();
    });
  }
  filtro.addEventListener('change', () => mostrarMes());
  mostrarMes();
  setInterval(() => {
    if (!document.hidden) mostrarMes(true);
  }, 30000);
})();
