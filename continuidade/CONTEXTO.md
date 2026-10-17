# Contexto, decisões e arquitetura

## Produto e equipe

O InPETU Hub é um projeto acadêmico da disciplina de Projeto Aplicado do UniSENAI, em parceria com o InPETU Hub. O sistema oferece agenda pública de espaços e área interna para reservas e relacionamento. A equipe apresentada no README é Bernardo Rocha, João Mussiol, João Feitosa e Guilherme Batista.

As pessoas previstas para os cinco acessos de demonstração são Milena, Bernardo, João F, João E e Guilherme. Nomes de login e instruções de acesso estão em `docs/ACESSOS.md`; não duplicar senhas nesta área. Apenas a conta de Bernardo tem as funções administrativas previstas na demonstração atual.

## Evolução resumida

| Linha/etapa | Decisão e contexto |
| --- | --- |
| 0.1 | Organizar a primeira entrega e explicar configuração/execução em Linux, macOS e Windows. Problemas iniciais incluíam caminhos e instalação no Ubuntu. |
| 0.2 | Introduzir melhorias de segurança, provisionamento de acessos e documentação de testes. Consultar o handover histórico para os detalhes daquela implementação. |
| 0.3 | Acrescentar logs e corrigir a experiência de login. Dificuldades com senhas e configuração de segundo fator levaram ao pedido de login básico na demonstração. |
| Versionamento | Pastas passaram a usar 0.1, 0.2, 0.3 e 0.4. O usuário quer entregas funcionais como GitHub Releases, com handover e descrição breve no README. |
| 0.4.0 | Trocar a instalação do usuário por VS Code/terminal por um iniciador nativo, com aplicativo e Java privado, configuração local e atualização automática de fixes da própria linha. |
| 0.4.1 | Corrigir a agenda que parecia totalmente ocupada quando não havia espaços ativos/disponíveis cadastrados. |
| 0.4.2 | Registrar automaticamente sessões, ações, erros e versões; preparar envio sem login GitHub do time e publicar resultados da CI em Testes. A hospedagem do receptor continua pendente. |
| 0.4.3 | Após backup autorizado e validado, manter uma única árvore de fontes ativos em aplicativo/, iniciador/ e receptor/, com guias em docs/. Retirar versões antigas e caminhos de início por terminal da árvore principal, preservando releases e histórico. |

A tabela resume decisões e motivação, não reproduz todo o chat nem comprova cada pedido como entregue. Versões e detalhes verificáveis estão nas tags, nos handovers e nas execuções de CI. O pedido antigo de senhas privadas foi posteriormente substituído pela autorização de acessos públicos de demonstração; isso não autoriza publicar segredos reais.

## Decisões de uso e colaboração

- Priorizar instalação simples e login que funcione para a equipe. Usuário final abre o executável; desenvolvedores podem usar ferramentas de código.
- Preservar as três famílias de sistema operacional e as duas arquiteturas dos pacotes. A matriz testa runners específicos, não todas as distribuições/edições existentes.
- Preservar dados entre reinícios e fixes. A linha 0.4 tem dados separados da 0.3 e não importa nem apaga a instalação anterior automaticamente.
- O pedido de logs é automático ao abrir/usar o executável; não criar um modo de teste que dependa de botão.
- Cada registro deve permitir identificar as versões em uso, inclusive durante a atualização do iniciador. Antes da seleção do pacote, appVersion pode aparecer como unknown.
- O envio de relatórios não deve pedir login GitHub no computador de cada integrante. Não distribuir token de escrita para resolver isso.
- Atender ao escopo exato do pedido. Análise não é autorização de mudança. Resolver detalhes reversíveis de implementação sem repetir perguntas já respondidas; pendências de conta/serviço devem ser conferidas na ferramenta em uso.
- Manter README profissional, com versão atual descrita brevemente, política de segurança para o público em linguagem simples e menção discreta de IA no rodapé.

## Componentes

| Componente | Papel |
| --- | --- |
| Java/Spring Boot | Monólito com Thymeleaf, Spring Security, JPA, Flyway, H2 e regras de agenda/reservas/CRM. O POM atual usa Spring Boot 4.1.1 e alvo Java 17; CI/pacotes usam JDK 21. |
| Iniciador Go | Interface local no navegador, instalação no perfil do usuário, controle do processo Java, atualização, snapshot, lock, logs e fila de relatórios. A CI fixa Go 1.27.2; conferir os pins antes de mudar. |
| Receptor Node | Recebe apenas diagnósticos estruturados/sanitizados; fila temporária limitada; não possui credencial de escrita do GitHub. |
| GitHub Actions | Testes e pacotes, publicação de releases e coleta periódica de relatórios. OIDC protege leitura/ACK da fila; GITHUB_TOKEN escreve em Testes dentro do job. |

O controlador do iniciador e o backend local ficam em loopback. O receptor público no Render é apenas para relatórios: **não é a aplicação Java hospedada em produção**, não hospeda o banco do time e não torna os dados locais compartilhados entre computadores.

Apenas reservas aprovadas ocupam horários e consomem cota. Pagamentos e conferência da identidade institucional continuam manuais. Migrations e contratos existentes devem ser preservados; não recriar o banco como solução genérica.

## Dados e autenticação

Dados ficam em `InPETU-Hub/0.4` na área de configuração do usuário: LocalAppData no Windows, Application Support no macOS ou configuração do usuário no Linux. Incluem `data/inpetu-v02.mv.db`, `.env.local`, `.secrets/mfa.key`, logs, backups e versões instaladas. O nome histórico mfa.key foi mantido e não prova que exista segundo fator obrigatório.

As contas locais de demonstração usam BCrypt e são restauradas ao padrão ao iniciar. Não é necessário arquivo particular de senhas para a demonstração. Alterações de senha podem voltar ao padrão no próximo início, conforme o comportamento documentado dessa linha. Esse modelo não é autorização para remover controles dos perfis de manutenção/produção.

Não publicar bancos, backups, configurações locais, chaves, logs brutos nem credenciais reais. A política pública está em `.github/SECURITY.md`. Ler os guias de recuperação antes de mexer em autenticação ou armazenamento.

## Logs e envio

O iniciador mantém `logs/sessoes/testeNN-data-id`, com eventos incrementais, metadados, relatório final e log legível. Fechar só a aba não finaliza a sessão; o botão Fechar iniciador encerra. Uma interrupção é recuperada na próxima abertura. O número local e o número global no GitHub podem ser diferentes.

A cópia local permanece até confirmação final de publicação. HTTP 202 significa fila, não entrega. O receptor pode perder sua fila em reinícios; sem cópia local/reabertura não se deve prometer entrega garantida. O plano gratuito pode suspender o processo e atrasar a primeira conexão.

Os relatórios públicos omitem dados livres e incluem códigos, IDs locais, tipos de erro e pontos do código. Não atestam identidade/integridade do dispositivo. Há limites de eventos/tamanho/fila; eventos omitidos por limite são contados. CONCLUIDO significa encerramento normal; erros individuais precisam ser conferidos.

Detalhes de limites, formatos e operação estão em `docs/RELATORIOS.md`, `docs/RECEPTOR.md` e `Testes/README.md`.

## Forma de continuidade a partir da reorganização

O usuário determinou uso somente pelo executável, uma etapa de trabalho por vez e nova release para cada entrega funcional. Backup foi a primeira etapa e está concluído; a reorganização foi autorizada em seguida. Depois da release 0.4.3, parar e aguardar autorização. Não aproveitar a etapa para ativar Render, redesenhar autenticação ou migrar dados. Os comandos de código e os testes continuam necessários ao desenvolvimento do executável.

## Android experimental independente — 17/10/2026

O usuário escolheu que o Android funcione sozinho. O código fica em `android/`, com SQLite próprio e linha `0.1.0-experimental.1`, separada da estável desktop `0.4.5`. Os dados não são sincronizados. A primeira entrega inclui os fluxos principais de espaços/agenda/reservas e logs locais; não promete paridade integral, atualização automática nem envio ao GitHub. Consulte `docs/ANDROID.md` e `docs/HANDOVER-ANDROID.md`. Publicar como prerelease, sem substituir a release estável ou seus assets. A assinatura Android deve ser preservada nas próximas versões; a chave privada de recuperação permanece fora do Git.
