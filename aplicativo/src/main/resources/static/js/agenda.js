(() => {
  const grid = document.getElementById('calendario');
  const title = document.getElementById('mes-titulo');
  const details = document.getElementById('detalhe-dia');
  const filter = document.getElementById('filtro-espaco');
  const notice = document.getElementById('agenda-aviso');
  const picker = document.getElementById('filtro-dia');
  const previous = document.getElementById('mes-anterior');
  const next = document.getElementById('mes-proximo');

  let month = new Date();
  month.setDate(1);
  let selected = null;
  let request = 0;
  const dateISO = d =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

  const today = grid.dataset?.today || dateISO(new Date());
  const minMonth = today.slice(0, 4) + '-01';
  const maxDate = grid.dataset?.maxDate || `${Number(today.slice(0, 4)) + 1}-12-31`;
  const maxMonth = maxDate.slice(0, 7);
  month = new Date(today + 'T12:00:00');
  month.setDate(1);

  const el = (tag, text, cls) => {
    const n = document.createElement(tag);
    if (text) n.textContent = text;
    if (cls) n.className = cls;
    return n;
  };

  // automatic === true só na atualização periódica: sem cookie, ela não renova a sessão de quem
  // está logado (o tempo de inatividade continua valendo). A resposta é a mesma para todos.
  async function render(automatic) {
    const seq = ++request;
    const shown = dateISO(month).slice(0, 7);
    previous.disabled = shown <= minMonth;
    next.disabled = shown >= maxMonth;
    if (picker) picker.value = selected || '';
    const monthName = month.toLocaleDateString('pt-BR', {month: 'long'});
    title.textContent = `${monthName.charAt(0).toUpperCase()}${monthName.slice(1)} ${month.getFullYear()}`;
    grid.replaceChildren(el('p', 'Carregando...'));
    try {
      const ym = dateISO(month).slice(0, 7);
      const query = new URLSearchParams({mes: ym});
      if (filter.value) query.set('espacoId', filter.value);
      const response = await fetch('/disponibilidade?' + query, automatic === true ? {credentials: 'omit'} : undefined);
      if (!response.ok) throw new Error('Não foi possível consultar a agenda.');
      const json = await response.json();
      if (seq !== request) return;

      const days = new Map(json.dias.map(d => [d.data, d]));
      // O aviso de cadastro de espaços só existe na página da equipe logada.
      if (notice) notice.hidden = !json.dias.length || !json.dias.every(d => d.estado === 'SEM_ESPACOS');
      grid.replaceChildren();
      const start = new Date(month);
      start.setDate(1 - start.getDay());
      for (let i = 0; i < 42; i++) {
        const d = new Date(start);
        d.setDate(start.getDate() + i);
        if (i >= 35 && d.getMonth() !== month.getMonth()) break;
        grid.append(dayButton(d, days.get(dateISO(d))));
      }
      if (selected && days.has(selected)) show(days.get(selected));
      else details.replaceChildren(el('p', 'Selecione uma data para consultar os espaços, capacidades e horários.'));
    } catch (e) {
      if (seq === request) {
        // Sem dados atuais, os horários e links de reserva da consulta anterior não podem continuar visíveis.
        details.replaceChildren(el('p', 'A agenda não pôde ser atualizada. Tente consultar o mês novamente.'));
        grid.replaceChildren(el('p', e.message, 'alert error'));
      }
    }
  }

  function dayButton(d, info) {
    const iso = dateISO(d);
    const b = el('button', null, 'day');
    b.type = 'button';
    b.append(el('strong', String(d.getDate())));
    if (!info) {
      b.classList.add('other');
      b.disabled = true;
      return b;
    }
    // Como na referência da equipe: dia com atividade marcada fica azul com um ponto; os demais, brancos.
    // Disponibilidade e horários aparecem só ao clicar no dia.
    const events = info.espacos.reduce((total, room) => total + room.ocupados.length, 0);
    if (events) {
      b.classList.add('has-events');
      b.append(el('span', '•', 'event-dot'));
    }
    if (iso === today) b.classList.add('today');
    if (iso < today) b.classList.add('past');
    if (iso === selected) b.classList.add('selected');
    const activity = events
      ? `${events} ${events === 1 ? 'atividade marcada' : 'atividades marcadas'}`
      : 'sem atividades marcadas';
    const situation =
      info.estado === 'SEM_ESPACOS' ? ', sem espaços habilitados' : info.estado === 'INDISPONÍVEL' ? ', sem horários livres' : '';
    b.setAttribute('aria-label', `${d.toLocaleDateString('pt-BR')}: ${activity}${situation}${info.feriado ? ', feriado' : ''}`);
    b.setAttribute('aria-pressed', iso === selected ? 'true' : 'false');
    b.addEventListener('click', () => {
      selected = iso;
      if (picker) picker.value = iso;
      grid.querySelectorAll('.day').forEach(x => {
        x.classList.remove('selected');
        x.setAttribute('aria-pressed', 'false');
      });
      b.classList.add('selected');
      b.setAttribute('aria-pressed', 'true');
      show(info);
    });
    return b;
  }

  function show(info) {
    details.replaceChildren();
    const formatted = new Date(info.data + 'T12:00:00').toLocaleDateString('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long'
    });
    details.append(el('h3', formatted + (info.feriado ? ' · Feriado' : '')));
    if (info.data < today) {
      details.append(el('p', 'Data passada: consulte os eventos. Novas reservas não são permitidas.', 'muted'));
    }
    if (!info.espacos.length) {
      details.append(
        el('p', 'Não há espaços habilitados para reservas nesta consulta. Isso não indica que os horários estejam ocupados.')
      );
    }
    for (const room of info.espacos) details.append(roomCard(info, room));
  }

  function roomCard(info, room) {
    const card = el('section', null, 'space-slots');
    card.append(el('h4', room.nome + (room.mobiliario ? ` · ${room.mobiliario}` : ` · até ${room.capacidade} pessoas`)));
    if (room.ocupados.length) {
      card.append(el('p', 'Horários indisponíveis', 'muted'));
      for (const event of room.ocupados) card.append(el('p', `${event.inicio}–${event.fim} · ${event.titulo}`, 'busy'));
    }
    card.append(el('p', room.livres.length ? 'Horários disponíveis' : 'Sem horários livres', 'muted'));
    for (const slot of room.livres) {
      if (info.data >= today) {
        const a = el('a', `${slot.inicio}–${slot.fim}`, 'button secondary slot');
        a.href = '/reserva?' + new URLSearchParams({data: info.data, espacoId: room.id, horaInicio: slot.inicio, horaFim: slot.fim});
        card.append(a);
      } else {
        card.append(el('span', `${slot.inicio}–${slot.fim}`, 'slot'));
      }
    }
    if (info.data >= today && room.livres.length) card.append(requestForm(info, room));
    return card;
  }

  function requestForm(info, room) {
    const form = el('form', null, 'slot-form');
    form.action = '/reserva';
    form.method = 'get';
    for (const [name, value] of Object.entries({data: info.data, espacoId: room.id})) {
      const input = el('input');
      input.type = 'hidden';
      input.name = name;
      input.value = value;
      form.append(input);
    }
    for (const [name, text, value] of [
      ['horaInicio', 'Início *', room.livres[0].inicio],
      ['horaFim', 'Fim *', room.livres[0].fim]
    ]) {
      const label = el('label', text);
      const input = el('input');
      input.type = 'time';
      input.name = name;
      input.value = value;
      input.required = true;
      label.append(input);
      form.append(label);
    }
    const button = el('button', 'Solicitar reserva');
    button.type = 'submit';
    form.append(button);
    return form;
  }

  previous.addEventListener('click', () => {
    if (dateISO(month).slice(0, 7) <= minMonth) return;
    month.setMonth(month.getMonth() - 1);
    selected = null;
    render();
  });
  next.addEventListener('click', () => {
    if (dateISO(month).slice(0, 7) >= maxMonth) return;
    month.setMonth(month.getMonth() + 1);
    selected = null;
    render();
  });
  if (picker) {
    picker.addEventListener('change', () => {
      const value = picker.value;
      if (!/^\d{4}-\d{2}-\d{2}$/.test(value) || value.slice(0, 7) < minMonth || value > maxDate) return;
      selected = value;
      month = new Date(value + 'T12:00:00');
      month.setDate(1);
      render();
    });
  }
  filter.addEventListener('change', render);
  render();
  setInterval(() => {
    if (!document.hidden) render(true);
  }, 30000);
})();
