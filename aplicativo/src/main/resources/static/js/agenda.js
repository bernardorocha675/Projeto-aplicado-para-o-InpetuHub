(()=>{
 const grid=document.getElementById('calendario'),title=document.getElementById('mes-titulo'),details=document.getElementById('detalhe-dia'),filter=document.getElementById('filtro-espaco'),notice=document.getElementById('agenda-aviso'),picker=document.getElementById('filtro-dia'),previous=document.getElementById('mes-anterior'),next=document.getElementById('mes-proximo');
 let month=new Date();month.setDate(1);let selected=null;let request=0;const dateISO=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
 const today=grid.dataset?.today||dateISO(new Date()),minMonth=today.slice(0,4)+'-01',maxDate=grid.dataset?.maxDate||`${Number(today.slice(0,4))+1}-12-31`,maxMonth=maxDate.slice(0,7);month=new Date(today+'T12:00:00');month.setDate(1);const el=(tag,text,cls)=>{const n=document.createElement(tag);if(text)n.textContent=text;if(cls)n.className=cls;return n;};
 async function render(){const seq=++request;const shown=dateISO(month).slice(0,7);previous.disabled=shown<=minMonth;next.disabled=shown>=maxMonth;if(picker)picker.value=selected||'';title.textContent=month.toLocaleDateString('pt-BR',{month:'long',year:'numeric'});grid.replaceChildren(el('p','Carregando...'));try{
  const ym=dateISO(month).slice(0,7);const query=new URLSearchParams({mes:ym});if(filter.value)query.set('espacoId',filter.value);
  const response=await fetch('/disponibilidade?'+query);if(!response.ok)throw new Error('Não foi possível consultar a agenda.');const json=await response.json();if(seq!==request)return;
  const days=new Map(json.dias.map(d=>[d.data,d]));notice.hidden=!json.dias.length||!json.dias.every(d=>d.estado==='SEM_ESPACOS');grid.replaceChildren();const start=new Date(month);start.setDate(1-start.getDay());
  for(let i=0;i<42;i++){const d=new Date(start);d.setDate(start.getDate()+i);if(i>=35&&d.getMonth()!==month.getMonth())break;const iso=dateISO(d),info=days.get(iso);const b=el('button',null,'day');b.type='button';b.append(el('strong',String(d.getDate())));
   if(!info){b.classList.add('other');b.disabled=true;}else{const partial=info.estado==='PARCIAL',full=info.estado==='INDISPONÍVEL',empty=info.estado==='SEM_ESPACOS';if(partial)b.classList.add('partial');if(full)b.classList.add('full');if(empty)b.classList.add('empty-day');b.append(el('span',empty?'— Sem espaços':partial?'◐ Parcial':full?'● Ocupado':'○ Livre'));if(iso===today)b.classList.add('today');if(iso<today)b.classList.add('past');if(iso===selected)b.classList.add('selected');b.setAttribute('aria-label',`${d.toLocaleDateString('pt-BR')}: ${empty?'Sem espaços habilitados':info.estado}${info.feriado?', feriado':''}`);b.setAttribute('aria-pressed',iso===selected?'true':'false');b.addEventListener('click',()=>{selected=iso;if(picker)picker.value=iso;grid.querySelectorAll('.day').forEach(x=>{x.classList.remove('selected');x.setAttribute('aria-pressed','false');});b.classList.add('selected');b.setAttribute('aria-pressed','true');show(info);});}
   grid.append(b);
  }
  if(selected&&days.has(selected))show(days.get(selected));else details.replaceChildren(el('p','Selecione uma data para consultar os espaços, capacidades e horários.'));
 }catch(e){if(seq===request){grid.replaceChildren(el('p',e.message,'alert error'));}}}
 function show(info){details.replaceChildren();const formatted=new Date(info.data+'T12:00:00').toLocaleDateString('pt-BR',{weekday:'long',day:'numeric',month:'long'});details.append(el('h3',formatted+(info.feriado?' · Feriado':'')));
  if(info.data<today)details.append(el('p','Data passada: consulte os eventos. Novas reservas não são permitidas.','muted'));
  if(!info.espacos.length)details.append(el('p','Não há espaços habilitados para reservas nesta consulta. Isso não indica que os horários estejam ocupados.'));
  for(const room of info.espacos){const card=el('section',null,'space-slots');card.append(el('h4',room.nome+` · até ${room.capacidade} pessoas`));
   if(room.ocupados.length){card.append(el('p','Horários indisponíveis','muted'));for(const event of room.ocupados)card.append(el('p',`${event.inicio}–${event.fim} · ${event.titulo}`,'busy'));}
   card.append(el('p',room.livres.length?'Horários disponíveis':'Sem horários livres','muted'));
   for(const slot of room.livres){const a=el('a',`${slot.inicio}–${slot.fim}`,'button secondary slot');a.href='/reserva?'+new URLSearchParams({data:info.data,espacoId:room.id,horaInicio:slot.inicio,horaFim:slot.fim});if(info.data>=today)card.append(a);else card.append(el('span',`${slot.inicio}–${slot.fim}`,'slot'));}
   if(info.data>=today&&room.livres.length){const form=el('form',null,'slot-form');form.action='/reserva';form.method='get';for(const [name,value]of Object.entries({data:info.data,espacoId:room.id})){const input=el('input');input.type='hidden';input.name=name;input.value=value;form.append(input);}
    for(const [name,text,value]of [['horaInicio','Início *',room.livres[0].inicio],['horaFim','Fim *',room.livres[0].fim]]){const label=el('label',text),input=el('input');input.type='time';input.name=name;input.value=value;input.required=true;label.append(input);form.append(label);}const button=el('button','Solicitar reserva');button.type='submit';form.append(button);card.append(form);}
   details.append(card);
  }
 }
 previous.addEventListener('click',()=>{if(dateISO(month).slice(0,7)<=minMonth)return;month.setMonth(month.getMonth()-1);selected=null;render();});next.addEventListener('click',()=>{if(dateISO(month).slice(0,7)>=maxMonth)return;month.setMonth(month.getMonth()+1);selected=null;render();});
 if(picker)picker.addEventListener('change',()=>{const value=picker.value;if(!/^\d{4}-\d{2}-\d{2}$/.test(value)||value.slice(0,7)<minMonth||value>maxDate)return;selected=value;month=new Date(value+'T12:00:00');month.setDate(1);render();});
 filter.addEventListener('change',render);render();setInterval(()=>{if(!document.hidden)render();},30000);
})();
