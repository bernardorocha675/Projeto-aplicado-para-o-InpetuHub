# Como testar sem precisar programar

Este roteiro usa os testes existentes e acrescenta verificações em um servidor real, com banco e contas descartáveis. Não pede as cinco senhas particulares e não usa os dados da instalação normal. Não precisa instalar Python para este comando.

## 1. Preparar

1. Atualize o projeto. Se você usou Git para baixar, abra o terminal na pasta do projeto e execute `git pull`. Se baixou ZIP, baixe a versão atual do repositório e extraia tudo em uma pasta nova.
2. No VS Code, abra a pasta **versão 0.3**. No menu Terminal, escolha Novo Terminal. Confira se está vendo `pom.xml`, `testar.sh` e `testar.bat` nessa pasta.
3. Se o servidor estiver rodando, pressione **Ctrl+C** no terminal dele e aguarde encerrar. O executor recompila o JAR.
4. Se ainda não configurou esta cópia, execute `bash configurar.sh` no Linux/macOS ou `.\configurar.bat` no Windows. Aguarde a mensagem de conclusão. A orientação de instalação das cinco contas continua em COMO-INICIAR.md.

## 2. Executar um comando

No **Linux/Ubuntu ou macOS**:

```bash
bash testar.sh
```

No **Windows**, pelo terminal PowerShell ou Prompt de Comando:

```powershell
.\testar.bat
```

Outra opção no VS Code: pressione **Ctrl+Shift+P** (no macOS, **Cmd+Shift+P**), procure **Tasks: Run Task / Tarefas: Executar Tarefa** e escolha **InPETU: testar**.

Não precisa abrir o site nem digitar senhas para essa rodada. Aguarde o terminal terminar; a primeira execução pode demorar vários minutos para baixar dependências. Ele avisa quando ainda está trabalhando. Não execute duas rodadas ao mesmo tempo.

## 3. Abrir o resultado

Abra a pasta `relatorios-testes` dentro de `versão 0.3`. Dê dois cliques em **ultimo.html** pelo Explorador de Arquivos ou Finder para abrir no Chrome, Edge, Firefox ou Safari. No VS Code, clicar no HTML pode abrir o código; nesse caso, use **Mostrar no Explorador / Revelar no Finder** e abra por lá.

O mesmo resumo fica em `relatorios-testes/ultimo.txt`. Uma nova execução substitui esses dois relatórios; copie o resultado anterior para uma pasta privada se quiser comparar rodadas.

| Resultado | O que significa | O que fazer |
|---|---|---|
| PASSOU | O resultado esperado foi confirmado nesta rodada. | Continue a lista. Isso não garante ausência de outros bugs. |
| FALHOU | Uma verificação não confirmou o comportamento esperado. | Anote o nome do teste e siga a seção de problemas abaixo. |
| NÃO EXECUTADO | Uma etapa anterior impediu este teste, ou ele foi ignorado. | Corrija a causa e repita. Não conte como aprovado. |
| Conferência manual | Precisa de pessoa, navegador, celular ou infraestrutura real. | Use o PDF privado e a lista ao fim do relatório. |

Se houver falha ou caso não executado, o comando termina com código de erro. Esse comportamento também permite ao GitHub sinalizar problemas automaticamente.

## 4. O que o comando faz

1. Compila o código e executa testes de regras, reservas, preços, cotas, conflitos, autorização, sessões, confirmação por email e expiração. Usa bancos de teste, sem esperar 24 horas para conferir a lógica de expiração.
2. Cria uma instalação descartável com contas sintéticas e uma porta local livre. Não depende da porta 8080 nem exige que as senhas particulares continuem iguais às iniciais.
3. Inicia o servidor, consulta a agenda, testa autenticação de administrador e colaborador, permissões, CSRF, remoção das rotas de segundo fator, URLs codificadas, repetição de erros locais e troca de senha de uma conta sintética.
4. Reinicia o servidor descartável e confere a persistência. Cria e restaura backup do H2 de teste. Recusa senha incorreta, arquivo adulterado, sobrescrita e backup com o servidor ainda aberto.
5. Encerra os processos que iniciou, remove os bancos/contas temporários e gera o relatório. O relatório informa separadamente qualquer falha nessa limpeza.

As ferramentas ficam em `.tools`. Os arquivos temporários e logs desta rodada ficam em `.tools/testes/rodada-...`. Esses diretórios e os relatórios são ignorados pelo Git. Não há alteração no código da aplicação nem importação das contas do time durante os testes.

Se interromper com Ctrl+C ou o computador desligar, podem sobrar dados sintéticos dentro de `.tools/testes`. Depois de confirmar que a rodada encerrou, você pode remover **somente** essa pasta de testes. Não apague `.secrets` ou `data` da instalação normal.

## 5. Relação com os 24 testes do PDF

| Testes do PDF | Cobertura do comando | O que ainda conferir pessoalmente |
|---|---|---|
| T01-T02: configurar e configurar novamente | Compila e testa a cópia já configurada. A CI repete o comando em seis plataformas. | Instalação limpa, downloads e preservação dos dados no computador de cada pessoa. |
| T03-T05: contas e logout | Login sintético, permissões e proteção de páginas; acesso de contas com segundo fator antigo sem exigir código. | Os cinco acessos reais e o comportamento de voltar no navegador. |
| T06-T07: erros, agenda e telas | Início do servidor, API, cabeçalhos e renderização de páginas nos testes de integração. | Mensagens claras, cliques, filtros, aparência e tela pequena em navegador real. |
| T08-T12: solicitações, aprovação, preços e cotas | Regras de serviço, validação de identidade e concorrência no banco de teste. | Confirmar se as regras cadastradas representam a operação desejada pelo responsável. |
| T13-T14: permissões e tentativas de login | Integração e HTTP real, incluindo caminhos codificados e ausência de bloqueio temporário no modo local. | Conferência visual das mensagens apresentadas ao usuário. |
| T15: entradas inválidas e scripts | Há validação de campos nos testes, mas não uma exploração exaustiva de entradas maliciosas. | Faça o cenário de texto malicioso do PDF e confira a exibição no navegador. |
| T16-T19: CSRF, senha e revogação | Integração com contas sintéticas; CSRF e troca de senha por HTTP real; rotas antigas de segundo fator indisponíveis. | Operação das telas administrativas pela equipe. |
| T20: duração das sessões | Simula o prazo absoluto de duas horas para ambos os perfis; não aplica o antigo prazo de MFA pendente. | Inatividade real de 20 minutos e comportamento do navegador durante a espera. |
| T21: confirmação de email | Testa limites, uso único e expiração do código sem email real. | Entrega SMTP, contato institucional correto e caixa de spam. |
| T22: expiração em 24 horas | Executa a lógica com datas de teste e recusa aprovação fora do prazo. | Agendamento e passagem do tempo no ambiente em uso. |
| T23: persistência e auditoria | Reinicia H2 de teste, confere login e cobre auditoria por integração. | Conferência dos registros esperados pelo responsável. |
| T24: backup e restauração | Testa backup H2 real descartável, integridade e situações de recusa. | Backup e restauração do MySQL de produção, se for utilizado. |

O teste HTTP não controla um navegador gráfico. Ele não comprova aparência, acessibilidade, execução de JavaScript ou todas as interações de tela. Os testes automáticos também não comprovam certificado HTTPS ou SMTP reais.

## 6. Se der problema

| Situação | Como proceder |
|---|---|
| `testar` não foi encontrado | Extraia o ZIP completo/atualize o projeto e abra o terminal em versão 0.3. |
| Solicita o configurador | Execute o comando de configurar do seu sistema, espere terminar e repita testar. |
| Falha ao baixar arquivo, internet ou Maven | Confira conexão/proxy. Repita depois de resolver a conexão; não desative certificados ou checksums. |
| Outra rodada está usando a instalação | Aguarde o outro terminal terminar. Não execute duas vezes em paralelo. |
| Falha de compilação ou de um teste | Abra ultimo.html; anote o nome da verificação e o sistema. O responsável técnico pode consultar os logs privados indicados no terminal. |
| Falha na limpeza dos temporários | Encerre os processos desta rodada e remova somente `.tools/testes` depois disso. |

Para relatar um bug, envie: nome do teste, sistema, navegador se aplicável, passos para reproduzir, esperado e observado. Revise qualquer log antes de compartilhar. **Não envie o PDF de senhas, arquivos `.env`, `.secrets`, bancos ou chaves.** O relatório HTML/TXT foi feito para não incluir valores de credenciais; logs detalhados merecem revisão adicional.

## Logs da versão 0.3

O mesmo comando também verifica o arquivo persistente, o identificador gerado pelo servidor e a ausência de credenciais sintéticas, parâmetros e cabeçalhos enviados no teste. JUnit verifica rotação compactada, JSON válido, descarte de mensagens livres e espelhamento da auditoria apenas após commit. Os logs da instalação descartável são removidos junto com os dados de teste; os logs do executor continuam privados em `.tools/testes/`.

## Correção de acesso e exibição de senha

A versão 0.3 possui botão Mostrar/Ocultar senha nos formulários de senha. Para login inválido, consulte [ACESSOS.md](ACESSOS.md): os comandos `acessos.sh`/`acessos.bat` conferem o arquivo privado com o banco local e permitem recuperação explícita, com servidor parado e confirmação após backup. Não apagam reservas/cadastros, não imprimem credenciais e preservam o perfil e os dados das contas existentes. Reiniciar normalmente continua sem redefinir senhas.

## Correção 0.3.1fix

Os cenários de segundo fator do PDF anterior deixam de se aplicar nesta release. Confira que o login com senha abre o painel, inclusive em contas que já usavam autenticador, e que o menu não oferece Segundo fator. As demais verificações do roteiro continuam válidas.

## Login básico 0.3.2

O roteiro também verifica usuário com maiúsculas/acentos/espaços, senha de oito caracteres no cadastro e na troca, ausência de bloqueio por erros locais e importação de contas copiadas depois da primeira inicialização. A importação automática é exercitada em um banco H2 real já existente. Contas e senhas reais não são usadas pela CI.
