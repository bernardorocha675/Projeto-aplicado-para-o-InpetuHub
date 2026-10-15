# Login ou senha inválidos — versão 03

As contas não vêm do GitHub. Elas estão no pacote privado de acessos entregue ao responsável. Cada computador tem seu próprio banco. **O login é o código do arquivo privado, não o nome da pessoa.** A senha distingue maiúsculas/minúsculas; não acrescente espaços. O botão Mostrar senha permite conferir a digitação e volta a ocultar ao enviar, trocar de aba ou sair da página. Ele também aparece nos formulários de alteração de senha.

## Por que pode acontecer

Se o sistema foi configurado/iniciado sem o pacote privado, ele gerou outro administrador aleatório. Copiar o pacote depois não troca as contas do banco: a importação inicial é feita uma vez. Também pode haver senha já alterada, conta desativada, erro de cópia ou terminal em outra versão. A mensagem do site é genérica e não distingue essas situações para quem está fora.

## 1. Usar a pasta que contém os dados

Atualize o código e abra **versão 03** no VS Code. Preserve `data/`, `.env.local` e `.secrets/mfa.key` da instalação em uso. Não substitua a pasta inteira por um ZIP vazio e não apague o banco.

Pare o servidor no terminal com Ctrl+C. Confira se não existe outra execução aberta. Os comandos abaixo só funcionam com o H2 local e o servidor parado; não iniciam o site nem expõem uma rota de recuperação na internet.

## 2. Conferir o arquivo privado

Extraia o pacote privado de acessos dentro de **versão 03**. O resultado precisa ser `.secrets/accounts.properties`, junto da chave MFA que já existia. Se o pacote se chama V02, ele também serve para a versão 03: o formato das contas é o mesmo. **Não substitua a chave MFA existente.** O ZIP de contas entregue contém o arquivo de contas, não uma nova chave MFA.

Se `.secrets/accounts.properties` já existe e contém o administrador aleatório, guarde antes uma cópia privada fora do repositório. Extraia/substitua apenas o arquivo de contas com o pacote correto. O arquivo pode conter as senhas iniciais em texto; não o envie em prints, issues ou commits.

Se ainda faltarem ferramentas/dependências, execute `bash configurar.sh` ou `.\configurar.bat`. Em seguida, confira:

| Linux/macOS | Windows PowerShell/CMD |
|---|---|
| `bash acessos.sh verificar` | `.\acessos.bat verificar` |

O comando mostra apenas quantidades: contas no arquivo, ausentes no banco, inativas e senhas diferentes. Não imprime nomes, logins, senhas, hashes ou MFA. Conferir não redefine senhas. Saída 0: confere; 2: existe diferença; 1: não conseguiu conferir (pasta, arquivo, senha do banco, banco em uso ou estrutura incompatível).

Se tudo confere, copie o login e a senha exatos do arquivo privado no seu computador e tente novamente usando Mostrar senha. Se já alterou sua senha pelo painel, a diferença para a senha inicial pode ser normal: use a senha atual. Aguarde o prazo de bloqueio temporário se houve muitas tentativas. Não use a recuperação apenas para conferir.

## 3. Recuperar sem apagar os dados

Use esta etapa **somente se deseja importar contas ausentes e voltar às senhas do arquivo privado**. O responsável pelo computador/banco deve fazê-la. Ela reativa as contas listadas e redefine suas senhas; não altera contas fora do arquivo. Perfis e MFA de contas existentes são preservados. Contas novas recebem o perfil definido no arquivo privado. Sessões antigas das contas afetadas são revogadas quando o site voltar.

Primeiro faça um backup cifrado, com o servidor parado:

```sh
# Linux/macOS; escolha uma senha forte quando o terminal solicitar.
bash backup.sh backup ../antes-recuperar.inpetu
```

No Windows: `.\backup.bat backup ..\antes-recuperar.inpetu`. Guarde esse arquivo e sua senha em local privado. Não coloque a senha na linha de comando. Confirme que o backup terminou antes de continuar.

| Linux/macOS | Windows PowerShell/CMD |
|---|---|
| `bash acessos.sh recuperar` | `.\acessos.bat recuperar` |

O programa mostra o resumo e solicita confirmação. Depois de fazer o backup, digite exatamente **RECUPERAR COM BACKUP** e pressione Enter. Qualquer outra resposta cancela. Essa confirmação declara que você fez o backup; o comando não gera um backup automaticamente. As alterações de contas e a auditoria acontecem na mesma transação. Falha cancela a transação. O histórico de reservas/cadastros não é apagado.

Depois rode o comando verificar novamente: deve informar que os acessos conferem.

## 4. Iniciar e entrar

Linux/macOS: `bash iniciar.sh`. Windows: `.\iniciar.bat`. Abra http://localhost:8080/login e use o login/senha do arquivo privado. Os administradores continuam precisando do autenticador; a recuperação não remove MFA nem cria senha mestre. Se o MFA já estava ativo, use o autenticador/código de recuperação existente.

A operação fica registrada na auditoria como `ACESSO_RECUPERADO_LOCAL`, sem credenciais. O administrador aleatório anterior, caso exista e não esteja no arquivo importado, não é apagado: depois de conferir os acessos, o administrador pode desativá-lo pelo painel.

Se a conferência não abrir o banco, não troque a senha em `.env.local` por chute. Use a configuração original que abriu esse banco. Para MySQL/produção, faça a manutenção pelo administrador e pelo procedimento do servidor; este comando atende somente H2 local.
