# InPETU Hub 0.5.12

Área Interna com as cores e os componentes do site oficial, e dependências atualizadas.

- **Uma só paleta de cores.** O sistema inteiro usa as mesmas cores, com os tons do site do InPETU Hub. O texto passou para o tom escuro do site.
- **Foco do teclado visível.** O contorno que mostra onde está o teclado era laranja-claro e quase sumia; agora é escuro em todo o sistema e branco no rodapé escuro.
- **Área Interna mais legível.** O item atual do menu, os sobretítulos, o cabeçalho das tabelas, as situações das reservas e os avisos passaram no contraste exigido para quem enxerga pouco (padrão WCAG AA).
- **Campos, botões e cards no padrão oficial.** Campos com borda visível e 48 px de altura, botões de 46 px com a sombra do botão do site, cards com cantos menores e caixas de marcar maiores (24 px, antes 13 px).
- **Avisos anunciados.** Avisos e erros ganharam uma faixa lateral colorida, e os que eram ignorados por leitores de tela agora são anunciados.
- **Toques maiores na Área Interna.** No celular, botões, caixas de marcar e links soltos (como Limpar e Consultar CRM) têm pelo menos 44 px.
- **Dependências atualizadas.** Spring Boot 4.1.2 (com Spring Framework 7.0.10, Spring Security 7.1.2, Hibernate 7.4.13 e Tomcat 11.0.27) e libphonenumber 9.0.42 (dados de telefone de vários países). Sem alertas de vulnerabilidade conhecidos.

Dados, reservas, contas, configurações, chave e backups permanecem na mesma pasta (`InPETU-Hub/0.4`); quem está na 0.5.x ou na 0.4.17 recebe esta versão automaticamente, com backup antes da troca. Sem migration nem reset. Windows/macOS/Linux x64/ARM64 e Java privado preservados. No macOS, o iniciador continua exigindo macOS 13 ou mais recente.

Publicação condicionada à matriz obrigatória de RELEASES.md.
