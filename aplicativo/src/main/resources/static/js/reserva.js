const cnpj = document.getElementById('cnpj');
if (cnpj) {
  cnpj.addEventListener('input', () => {
    cnpj.value = cnpj.value.replace(/[^0-9]/g, '').slice(0, 14);
  });
}
