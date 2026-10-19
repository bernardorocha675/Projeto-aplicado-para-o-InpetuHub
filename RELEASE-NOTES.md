# InPETU Hub 0.4.13

Agenda mais segura após falhas, regras de contato unificadas e código padronizado.

- Se a agenda não conseguir atualizar a consulta, os horários e botões de reserva do dia selecionado anteriormente deixam de aparecer, com aviso para consultar de novo. Antes, eles continuavam visíveis sem dados atuais.
- O email de colaboradores passa a seguir o mesmo formato do formulário público e do cadastro de empreendedores. A regra de usuário (login) também é única para a tela, o arquivo privado de contas e a manutenção.
- Código do iniciador e scripts da interface formatados no padrão de cada linguagem, sem mudança de comportamento; a CI passa a recusar código Go fora do padrão.

Dados, reservas, contas, configurações, chave e backups permanecem no mesmo local da família 0.4.x. Não há migration, reset, mudança de dependências ou ativação externa. Windows/macOS/Linux x64/ARM64 e Java privado preservados; Android continua retirado.

Publicação condicionada à matriz obrigatória de RELEASES.md.
