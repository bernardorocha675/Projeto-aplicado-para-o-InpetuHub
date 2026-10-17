# Mapa de trabalho e validação

## Arquivos de referência

Os caminhos abaixo partem da raiz do repositório. A pasta ativa possui espaço e acento: use aspas nos comandos.

| Área | Caminhos principais |
| --- | --- |
| Entrada Java | `versão 0.4/src/main/java/br/com/inpetu/hub/InpetuApplication.java` |
| Contas locais | `versão 0.4/src/main/java/br/com/inpetu/hub/config/LocalAccounts.java`, `PrivateAccountInitializer.java` no mesmo diretório |
| Regras e agenda | `versão 0.4/src/main/java/br/com/inpetu/hub/service/`, especialmente DisponibilidadeService e ReservaService |
| Rotas e telas | `versão 0.4/src/main/java/br/com/inpetu/hub/controller/`, `versão 0.4/src/main/resources/templates/` |
| Segurança e logs Java | `versão 0.4/src/main/java/br/com/inpetu/hub/security/` e `observability/` |
| Instalação/processo/atualização | `versão 0.4/launcher/main.go`, `model.go`, `app.go`, `update.go`, `ui.html` |
| Sessão e envio local | `versão 0.4/launcher/session.go`, `reporting.go`, `session_test.go` |
| Recepção/validação | `versão 0.4/receiver/server.mjs`, `schema.mjs`, `test.mjs` |
| Coletor e identidade | `.github/scripts/collect-reports.mjs`, `verify-collector-identity.mjs`, `.github/workflows/collect-test-reports.yml` |
| Empacotamento e smoke | `.github/scripts/package_v04.py`, `smoke_v04.py` |
| Publicação | `.github/workflows/release-v0.4.yml`, `.github/scripts/publish_v04.py`, `VERSION`, `RELEASE-NOTES.md`, `RELEASES.md` |
| Guias para pessoas | `versão 0.4/COMO-INICIAR.md`, `TESTAR.md`, `ACESSOS.md`, `ATUALIZACOES.md`, `HANDOVER.md` |

## Comandos de desenvolvimento

Estes comandos são para quem altera código, não para os usuários finais. Não assumem que as ferramentas já estejam instaladas. O primeiro build pode baixar dependências. Execute testes e smoke em dados descartáveis, sem usar o perfil real da equipe.

Na raiz, com Node disponível (receptor exige Node >=22; hospedagem planejada usa 24):

```sh
node .github/scripts/test_agenda_ui.cjs
node --test "versão 0.4/receiver/test.mjs"
```

Dentro de `versão 0.4/launcher`, com Go compatível com o pin da CI:

```sh
go test ./...
go vet ./...
```

Na raiz, com JDK/Maven configurados:

```sh
mvn -B -ntp -f "versão 0.4/pom.xml" test
```

Para o roteiro de integração existente, entre em `versão 0.4`. Linux/macOS usam `bash configurar.sh` e depois `bash testar.sh`; Windows usa `configurar.bat` e depois `testar.bat`. A configuração prepara ferramentas locais quando necessário, compila e testa. Os scripts usam dados de teste próprios; leia `TESTAR.md` e o código do roteiro antes de executá-los.

As ferramentas baixadas em `.tools` não ficam automaticamente no PATH de todos os terminais. Se o comando direto de Java/Maven não for encontrado, utilize os scripts de configuração/runtime ou configure explicitamente o ambiente, sem alterar o sistema desnecessariamente.

Empacotamento requer o JAR já gerado, Go, JDK com jlink e INPETU_GOOS/INPETU_GOARCH coerentes com o host. Use a matriz em `.github/workflows/release-v0.4.yml` como referência: gerar um pacote em um host não comprova seu funcionamento em outro. O smoke roda o executável real em diretório temporário, simula um fix futuro sem publicar release falsa e verifica banco/chave/logins/sessões.

## Publicar uma mudança funcional

1. Confira a release atual e escolha um número novo. Não reutilize nem mova uma tag publicada.
2. Atualize VERSION, POM, buildVersion de referência no Go e caminhos do JAR nos scripts afetados. Confira coerência antes de enviar.
3. Atualize release notes, handover, guias afetados e o estado de continuidade. Mantenha a descrição da versão no README curta.
4. Faça commit/revisão conforme o fluxo do repositório e confira os checks. O merge em main que aciona a linha 0.4 pode publicar automaticamente depois dos gates; não trate esse merge como mera simulação.
5. Só declare a entrega publicada depois de verificar Actions, tag, release e assets. A matriz exige seis ambientes nativos, verificações de código/dependências/segredos, CodeQL e publicação do relatório de CI.

Não executar manualmente o publicador como atalho para contornar a CI. Não incluir dados do usuário nos pacotes. Uma correção no runtime privado exige novo pacote; atualizar o Java do sistema não atualiza o Java embarcado.

## Retomar a ativação do receptor

Este é o próximo trabalho operacional pendente, **não uma ação autorizada por esta documentação**. Primeiro verificar se o usuário respondeu à confirmação de workspace e quais integrações o Claude Code consegue acessar. Não criar serviço duplicado nem assumir acesso à conta do ChatGPT.

Quando houver autorização e acesso:

1. Conferir `Testes/configuracao.json` e serviços existentes. Se o envio já tiver sido ativado depois deste registro, investigar o estado real antes de mudar.
2. Publicar somente o receptor Node no workspace confirmado, plano Free, branch main, com auto-deploy desativado. Não implantar o aplicativo Java, banco ou outros serviços como parte disso.
3. Usar NODE_VERSION=24. Build: `node --check 'versão 0.4/receiver/server.mjs'`. Start: `node 'versão 0.4/receiver/server.mjs'`. O serviço escuta em `0.0.0.0:$PORT`.
4. Confirmar deploy saudável e GET `/healthz` com versão correta. GET `/v1/pending` e POST `/v1/ack` sem identidade válida devem ser negados. Nunca registrar JWTs nos documentos/logs públicos.
5. Registrar a URL HTTPS real terminada em `/v1/reports` e `enabled: true` no arquivo público de configuração. Não inventar URL nem adicionar tokens. O iniciador consulta essa configuração; não é necessária reinstalação apenas para ativar o destino.
6. Abrir/usar/fechar uma instalação de demonstração, aguardar coleta pelo Actions e conferir a nova pasta `Testes/testeNN-data-id`, `log.txt`, `resumo.json` e versões por evento. Testar erro controlado e revisar ausência de dados privados.
7. Confirmar que HTTP 202 mantém o relatório pendente e que o recibo final/enviado.json só aparece depois da publicação. Conferir reenvio offline/interrompido e deduplicação.
8. Atualizar o estado de continuidade com URL pública, serviço, commit da configuração e evidências. Não salvar credenciais nem dados da conta.

A coleta é agendada aproximadamente a cada cinco minutos, com possíveis atrasos. O plano gratuito e a fila em memória não garantem entrega enquanto o computador estiver fechado e a fila tiver sido perdida. Guardar cópia local até a confirmação e manter auto-deploy desligado para que commits em Testes não reiniciem o receptor.

## Como encerrar uma sessão de trabalho

Registre o pedido atendido, os arquivos alterados, a validação feita, o que permanece sem teste e a próxima tarefa. Atualize `ESTADO-ATUAL.md` e o handover quando aplicável. Se faltarem PDF, acesso ou autorização exigida pela ferramenta, registre a dependência concreta sem inventar que a etapa foi concluída.
