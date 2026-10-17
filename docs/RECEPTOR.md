# Receptor de relatórios — 0.4.5

Serviço Node sem dependências externas. O processo não possui credencial de escrita do GitHub. Recebe diagnósticos estruturados em POST /v1/reports, valida tamanho/campos e responde 202 enquanto a cópia estiver somente na fila temporária.

GET /v1/pending e POST /v1/ack exigem JWT OIDC assinado pelo GitHub, audiência api://inpetu-report-collector, ID do repositório 1419952796, main e o workflow collect-test-reports.yml. Forks, PRs e outros workflows não leem a fila. GET /healthz informa estado e versão, sem relatórios.

O workflow usa GITHUB_TOKEN exclusivamente dentro do job para criar as pastas em Testes e só confirma após o push. A numeração global e os hashes evitam duplicação. O cliente retém os arquivos locais até o recibo final.

## Publicação no Render

Criar um Web Service no workspace confirmado pelo responsável, plano Free, runtime Node, repositório deste projeto e main. Configurar NODE_VERSION=24. Build: `node --check 'receptor/server.mjs'`. Start: `node 'receptor/server.mjs'`. O servidor escuta em 0.0.0.0 na porta PORT do Render.

Não requer chave privada, GitHub App ou token pessoal. Manter auto-deploy desativado para que commits de relatórios não reiniciem a fila. Depois de testar uma mudança do receptor, disparar um deploy manual. Conferir /healthz e ativar a URL HTTPS real em Testes/configuracao.json. A integração do Render exige confirmação do workspace antes de criar o serviço.

O plano gratuito pode levar cerca de um minuto para reativar e perde a fila em reinícios/deploys. Os relatórios ainda não confirmados ficam no computador e são reenviados durante o uso ou na próxima abertura. Não há garantia de entrega enquanto o computador estiver fechado e a fila temporária tiver sido perdida.

## Limites e confiança

A entrada de diagnósticos não atesta identidade ou integridade do computador. O coletor reconstrói os arquivos; nunca executa conteúdo recebido. Limites: 3 MiB/requisição, 5.000 eventos/sessão, quatro requisições simultâneas, 20 relatórios/40 MiB em fila, 50 novos relatórios/dia UTC e 120 recebimentos/origem a cada 15 minutos. A origem pode ser um proxy do provedor.

O índice permite até 5.000 sessões antes de exigir arquivamento. A coleta pode atrasar e requer Actions habilitado. Não usar como evidência autenticada de segurança nem publicar dados confidenciais. Ver Testes/README.md e RELATORIOS.md.

## Testes

Na raiz: `node --test 'receptor/test.mjs'`. O workflow valida também um token OIDC real antes de ler a fila. A matriz nativa verifica os logs reais produzidos pelo iniciador e pelo Java.
