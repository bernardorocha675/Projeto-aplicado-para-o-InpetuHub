'use strict';
document.querySelectorAll('input[type="password"]').forEach((input, index) => {
  if (!input.id) input.id = 'private-password-' + index;
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'secondary password-toggle';
  button.textContent = 'Mostrar senha';
  button.setAttribute('aria-controls', input.id);
  button.setAttribute('aria-pressed', 'false');
  button.setAttribute('aria-label', 'Mostrar senha');
  const hide = () => {
    input.type = 'password';
    button.textContent = 'Mostrar senha';
    button.setAttribute('aria-pressed', 'false');
    button.setAttribute('aria-label', 'Mostrar senha');
  };
  button.addEventListener('click', () => {
    if (input.type === 'text') hide();
    else {
      input.type = 'text';
      button.textContent = 'Ocultar senha';
      button.setAttribute('aria-pressed', 'true');
      button.setAttribute('aria-label', 'Ocultar senha');
    }
  });
  (input.closest('label') || input).insertAdjacentElement('afterend', button);
  input.form?.addEventListener('submit', hide);
  window.addEventListener('pagehide', hide);
  document.addEventListener('visibilitychange', () => { if (document.hidden) hide(); });
});
