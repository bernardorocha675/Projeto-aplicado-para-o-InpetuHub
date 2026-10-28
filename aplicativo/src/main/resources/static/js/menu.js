'use strict';
// Menu do cabeçalho: no celular, o botão abre e fecha o menu; os submenus abrem pelo botão ao lado do item,
// e não mais pela passagem do Tab. Sem este script, o menu continua como antes (aberto e com foco nos submenus).
const topo = document.querySelector('.topo');
const botao = topo && topo.querySelector('.menu-botao');
const menu = botao && document.getElementById(botao.getAttribute('aria-controls'));
if (topo && botao && menu) {
  const subBotoes = topo.querySelectorAll('.submenu-botao');
  const abrirMenu = aberto => {
    topo.dataset.menu = aberto ? 'aberto' : 'fechado';
    botao.setAttribute('aria-expanded', String(aberto));
  };
  const abrirSubmenu = (b, aberto) => {
    b.setAttribute('aria-expanded', String(aberto));
    document.getElementById(b.getAttribute('aria-controls'))?.classList.toggle('aberto', aberto);
  };
  abrirMenu(false);
  botao.hidden = false;
  botao.addEventListener('click', () => abrirMenu(botao.getAttribute('aria-expanded') !== 'true'));
  subBotoes.forEach(b => {
    b.hidden = false;
    b.addEventListener('click', () => {
      const aberto = b.getAttribute('aria-expanded') !== 'true';
      subBotoes.forEach(outro => abrirSubmenu(outro, false));
      abrirSubmenu(b, aberto);
    });
  });
  // Esc fecha primeiro o submenu aberto e depois o menu, devolvendo o foco a quem abriu.
  document.addEventListener('keydown', e => {
    if (e.key !== 'Escape') return;
    const sub = topo.querySelector('.submenu-botao[aria-expanded="true"]');
    if (sub) {
      abrirSubmenu(sub, false);
      sub.focus();
    } else if (botao.getAttribute('aria-expanded') === 'true') {
      abrirMenu(false);
      botao.focus();
    }
  });
}
