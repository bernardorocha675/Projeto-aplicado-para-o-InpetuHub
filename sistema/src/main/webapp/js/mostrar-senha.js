'use strict';
// Botão "Mostrar senha" ao lado de cada campo de senha (porta do password-toggle.js do antigo).
// A senha volta a ficar escondida ao enviar o formulário, ao trocar de aba e ao sair da página.
document.querySelectorAll('input[type="password"]').forEach((campo) => {
  const botao = document.createElement('button');
  botao.type = 'button';
  botao.className = 'botao secundario mostrar-senha';
  botao.setAttribute('aria-controls', campo.id);
  const esconder = () => {
    campo.type = 'password';
    botao.textContent = 'Mostrar senha';
    botao.setAttribute('aria-pressed', 'false');
  };
  esconder();
  botao.addEventListener('click', () => {
    if (campo.type === 'text') {
      esconder();
    } else {
      campo.type = 'text';
      botao.textContent = 'Ocultar senha';
      botao.setAttribute('aria-pressed', 'true');
    }
  });
  campo.insertAdjacentElement('afterend', botao);
  if (campo.form) {
    campo.form.addEventListener('submit', esconder);
  }
  window.addEventListener('pagehide', esconder);
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      esconder();
    }
  });
});
