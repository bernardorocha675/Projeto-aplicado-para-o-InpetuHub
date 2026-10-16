# InPETU Hub — versão 0.3

**Pacote Java: 0.3.3.** Entrega acadêmica com execução local validada, logs persistentes, diagnóstico/recuperação local de acessos e botão Mostrar/Ocultar senha. A implantação em produção permanece pendente.

Agenda pública e CRM em Java 17/21, Spring Boot 4.1.1, Spring Security, Thymeleaf, JPA e Flyway. O modo local usa H2 em arquivo. O perfil `prod` usa MySQL e exige HTTPS direto e SMTP.

Leia [COMO-INICIAR.md](COMO-INICIAR.md) para instalar e o [handover técnico](HANDOVER.md) para assumir a manutenção: arquitetura, operação, acessos, backup, atualização, evidências de testes e pendências. As credenciais reais são entregues fora do repositório; não existe usuário ou senha padrão.

## Configurar e executar

Abra **versão 0.3** no VS Code e use o terminal dessa pasta.

| Sistema | Configurar uma vez | Executar |
|---|---|---|
| Linux/macOS | `bash configurar.sh` | `bash iniciar.sh` |
| Windows PowerShell/CMD | `.\configurar.bat` | `.\iniciar.bat` |

Os scripts preparam Java e Maven, verificam downloads, criam configuração privada, executam os testes e iniciam o sistema em `http://localhost:8080`. Não é necessário instalar MySQL para a apresentação local. As tarefas do VS Code também funcionam quando a pasta aberta é versão 0.3.

Ao iniciar, o sistema procura automaticamente o fix mais recente da linha **0.3.x**, faz uma cópia de segurança e preserva os arquivos privados. Quem está na 0.3.2 precisa instalar este recurso uma vez. Consulte [ATUALIZACOES.md](ATUALIZACOES.md) para funcionamento, retorno e desativação.

Aplique o pacote privado conforme [ACESSOS.md](ACESSOS.md), ou copie seu arquivo de contas para `.secrets/accounts.properties`. Se ele não existir, o configurador cria um único administrador com login e senha aleatórios, guardados somente nesse arquivo local. Os valores não são impressos no terminal. Leia-os localmente e guarde-os em um gerenciador de senhas.

Ao iniciar, contas novas ou alteradas no arquivo privado são importadas, inclusive em bancos já existentes. Cada entrada é aplicada uma vez; arquivo idêntico não redefine uma senha trocada pelo painel nem reativa contas desativadas manualmente. Cada computador mantém seu próprio banco; o ZIP público contém código, sem contas reais.

## Segurança desta versão

- Senhas BCrypt, mínimo de 8 e máximo de 64 caracteres, com limite de 72 bytes.
- Sem credenciais padrão no código, exemplos ou documentação desta versão.
- Login e senha para administradores e colaboradores. O segundo fator permanece removido, inclusive para contas já configuradas; essa mudança reduz a proteção das contas administrativas.
- Desativação, mudança de login, senha ou perfil revogam sessões anteriores na próxima requisição. Sessões expiram por inatividade e têm limite absoluto de duas horas.
- No modo local, erros de senha não bloqueiam o login. Produção mantém limites de login; consultas e solicitações continuam limitadas. Endereços encaminhados pelo cliente não são confiados.
- CSRF, escape HTML, consultas parametrizadas, CSP, Referrer-Policy e restrições de recursos do navegador.
- Auditoria de autenticação, contas, cadastros, disponibilidade e operações privilegiadas; nenhum valor de senha ou token é registrado.
- Backup local cifrado, com exclusão mútua entre servidor e cópia/restauração.
- CI para testes nativos, varredura de segredos, análise CodeQL e consulta OSV das dependências resolvidas.

## Fluxo de reservas

1. O visitante consulta horários **aprovados** e envia uma solicitação. Dados declarados ficam separados do CRM, sem bloquear salas nem consumir cotas.
2. Quando SMTP está configurado, um código de confirmação é enviado ao contato institucional já cadastrado para um CNPJ existente; em um novo cadastro, ao email declarado. O código vale dez minutos, aceita no máximo cinco tentativas e funciona uma vez. A confirmação é feita no navegador que enviou a solicitação.
3. A equipe confere a identidade e o vínculo institucional usando um contato confiável e registra como fez a conferência. A confirmação de email sozinha não comprova representação da empresa. No modo local sem SMTP, essa conferência é manual; em produção, email confirmado também é obrigatório.
4. A solicitação conferida vai para Reservas → Pendentes. Apenas a aprovação bloqueia horário e consome cota, sempre recalculando preços e disponibilidade sob a transação com bloqueio de configuração.
5. Pedidos recebidos e reservas pendentes expiram após 24 horas. A tarefa roda a cada minuto; aprovação também verifica o prazo. Recusa/cancelamento de uma pendente sem alocação não gera crédito nem penalidade de cota.

Os parâmetros financeiros, taxas, expediente, capacidades, feriados e preços iniciais continuam sendo exemplos: confirme com o Hub antes de operar. Os pagamentos continuam manuais. Email de notificação não tem fila persistente/reenvio automático; falhas não desfazem reservas já confirmadas.

## Dados, testes e produção

O H2 fica em `data/inpetu-v02.mv.db`; preserve também `.env.local` e `.secrets/mfa.key`. A chave continua sendo usada em outras verificações do sistema; o nome do arquivo foi mantido por compatibilidade.

```sh
bash maven.sh clean verify
bash maven.sh dependency:list -DincludeScope=runtime -DoutputFile=target/dependencies.txt
python scripts/check_dependencies.py
```

No Windows, substitua `bash maven.sh` por `.\maven.bat`. Python 3 é necessário apenas para as verificações auxiliares; instalação e execução do aplicativo não dependem dele.

Para produzir um backup, pare o servidor e execute `bash backup.sh backup arquivo.inpetu` ou `.\backup.bat backup arquivo.inpetu`. A senha é solicitada pelo terminal. Veja a restauração e os requisitos de produção no handover.

Os resultados e limites estão em [docs/VALIDACAO.md](docs/VALIDACAO.md) e [docs/SEGURANCA.md](docs/SEGURANCA.md). As melhorias funcionais do PDF, como fotos das salas e adequação do rodapé original, continuam sendo um escopo separado; esta versão acrescenta logs técnicos às proteções da versão 0.2.

## Testar com um comando

Depois de configurar, execute `bash testar.sh` no Linux/macOS ou `.\testar.bat` no Windows. Abra `relatorios-testes/ultimo.html` no navegador. O executor usa banco e contas descartáveis, sem usar os acessos privados. Veja [TESTAR.md](TESTAR.md).

## Logs persistentes

Terminal e `logs/inpetu.log` em JSON; rotação diária/10 MB, gzip, histórico de 14 dias e limite de 200 MB de arquivos antigos. Mensagens livres e dados HTTP não são gravados. Identifique uma requisição pelo cabeçalho `X-Request-ID`. Leia [LOGS.md](LOGS.md) para consultar, interpretar e relatar erros.

Login recusado? Leia [ACESSOS.md](ACESSOS.md) para conferir se o pacote privado foi importado neste computador, sem apagar os dados.
