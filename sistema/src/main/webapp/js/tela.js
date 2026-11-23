'use strict';
// Avisa o servidor local do que aconteceu na página, só com códigos fixos: nunca lê o que foi digitado.
// Sem cookie (não renova a sessão) e, se falhar, não atrapalha a tela.
(() => {
  const scripts = ['agenda.js', 'confirmar.js', 'mostrar-senha.js', 'tela.js'];
  let avisos = 0;

  function avisar(evento, alvo, linha) {
    if (++avisos > 200) return;
    const dados = new URLSearchParams({ evento, alvo, pagina: location.pathname });
    if (Number.isInteger(linha) && linha > 0) dados.set('linha', linha);
    fetch('/tela?' + dados, { headers: { 'X-Inpetu-Tela': '1' }, credentials: 'omit', keepalive: true }).catch(() => {});
  }
  window.avisarTela = avisar;

  const daqui = (endereco) => {
    const nome = String(endereco || '').split('/').pop().split('?')[0];
    return scripts.includes(nome) ? nome : 'OUTRO_SCRIPT';
  };
  window.addEventListener('error', (e) => {
    if (e instanceof ErrorEvent) avisar('ERRO_SCRIPT', daqui(e.filename), e.lineno);
  });
  window.addEventListener('unhandledrejection', () => avisar('ERRO_SCRIPT', 'OUTRO_SCRIPT'));
  // O navegador recusou um campo antes de enviar: o pedido nem chega ao servidor.
  // Os campos vão juntos, num aviso só.
  let recusados = null;
  document.addEventListener(
    'invalid',
    (e) => {
      if (!recusados) {
        recusados = new Set();
        setTimeout(() => {
          avisar('CAMPO_INVALIDO', [...recusados].slice(0, 20).join(','));
          recusados = null;
        }, 0);
      }
      recusados.add(/^[A-Za-z]{1,30}$/.test(e.target.name || '') ? e.target.name : 'OUTRO_CAMPO');
    },
    true
  );
  document.addEventListener('click', (e) => {
    const alvo = e.target instanceof Element ? e.target.closest('a, button') : null;
    if (!alvo) return;
    if (alvo.matches('.dia')) avisar('CLIQUE', 'DIA');
    else if (alvo.id === 'mes-anterior') avisar('CLIQUE', 'MES_ANTERIOR');
    else if (alvo.id === 'mes-proximo') avisar('CLIQUE', 'MES_PROXIMO');
    else if (alvo.matches('a.horario')) avisar('CLIQUE', 'HORARIO');
    else if (alvo.matches('a[target="_blank"]')) avisar('CLIQUE', 'LINK_EXTERNO');
  });
  document.addEventListener('change', (e) => {
    if (e.target.id === 'filtro-espaco') avisar('CLIQUE', 'FILTRO_ESPACO');
    else if (e.target.id === 'filtro-dia') avisar('CLIQUE', 'FILTRO_DIA');
  });
})();
