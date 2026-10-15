# InPETU Hub — Agenda pública + CRM

MVP local em um único projeto Java 17, Spring Boot 3.5.6, MVC, Thymeleaf, Security, JPA, Validation, Mail e Maven. Banco H2 em arquivo no modo automático; MySQL continua disponível. Frontend em HTML, CSS e JavaScript puro. As duas interfaces compartilham os mesmos serviços e banco.

## Handover e clonagem

O estado desta versão, os pontos de manutenção e as pendências estão em
[HANDOVER.md](HANDOVER.md).

```sh
git clone https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub.git
cd Projeto-aplicado-para-o-InpetuHub
cd "versão 01"
```

Abra a subpasta `versão 01` no VS Code: o `pom.xml` e os comandos estão dentro dela.

## Rodar no VS Code com dois comandos

Se estiver usando o ZIP original, extraia-o e abra **a pasta `inpetu-hub`**. Se clonou o repositório, abra **a subpasta `versão 01`**. Em ambos os casos, o terminal precisa estar na pasta que contém `pom.xml`.
Use **Terminal → Novo Terminal**. Não execute de dentro do ZIP.

### macOS e Linux

Configure uma vez:

```sh
bash configurar.sh
```

Inicie sempre que quiser usar:

```sh
bash iniciar.sh
```

### Windows (PowerShell ou Prompt de Comando)

Configure uma vez:

```powershell
.\configurar.bat
```

Inicie sempre que quiser usar:

```powershell
.\iniciar.bat
```

Os `.bat` chamam o PowerShell com política de execução limitada àquele processo.
Não é necessário mudar a política do Windows, reiniciar o terminal ou executar como administrador.

O configurador reutiliza um JDK **17 ou 21** compatível, ou baixa Temurin 17 automaticamente.
Baixa também Maven **3.9.9**, verifica os arquivos por checksum, cria `.env.local`
sem substituir configurações existentes, baixa as dependências e roda os testes.
As ferramentas ficam dentro de `.tools/`; não é necessário configurar o PATH.
No macOS/Linux, são usados Bash, curl, tar e shasum/sha256sum/sha512sum do sistema.
É necessário acesso à internet na configuração; o download do JDK pode ser grande.
A execução faz uma compilação limpa para incluir suas alterações e então inicia o servidor.
Dependências novas também exigem internet.

Abra **http://localhost:8080/** (agenda) ou **http://localhost:8080/login** (CRM).
Aguarde a mensagem `Started InpetuApplication` no terminal. Pare com **Ctrl+C**.
Se mudar `PORT` em `.env.local`, use essa porta no navegador.

O modo automático usa **H2 em arquivo**, criado pelas migrations Flyway na primeira
inicialização e salvo em `data/inpetu.mv.db`. Fechar e abrir mantém os dados.
Não é MySQL: é uma alternativa para desenvolvimento e apresentação local.
O servidor escuta apenas na própria máquina. Não apague `data/` para atualizar o código.
Para fazer backup, pare o aplicativo e copie a pasta `data/` junto de `.env.local`.
Dados de um MySQL anterior não são importados automaticamente.

Também há tarefas em **Terminal → Executar Tarefa**: configurar tudo, executar,
testar e gerar JAR. As extensões recomendadas são opcionais para esses comandos.
F5 é opcional e depende de instalar as extensões Java/Spring e configurar o JDK
no VS Code; escolha `InPETU Hub (banco local)` depois da configuração inicial.

### Usar o MySQL original

A configuração padrão em `application.properties` continua usando MySQL. Para
usá-la, instale/inicie MySQL, execute `sql/criar-banco.sql` com sua senha e configure
`.env` a partir de `.env.example`. Após o configurador preparar Java/Maven:

```sh
# macOS/Linux
bash maven.sh spring-boot:run
```

```powershell
# Windows
.\maven.bat spring-boot:run
```

Esses comandos não ativam o perfil `local`. Não é necessário modificar as migrations.
O modo automático dos scripts `iniciar` sempre seleciona H2. Os dois bancos são independentes.

## Login inicial — somente desenvolvimento

- Login: `admin@inpetuhub.local`
- Senha: `Inpetu@123`
- Perfil: `ADMIN`

A senha entra no banco somente como hash BCrypt. Altere pelo painel Administração → Colaboradores. `ADMIN_LOGIN`/`ADMIN_PASSWORD` controlam a criação inicial, sem resetar contas existentes. `SEED_DEMO=false` desativa o cadastro de exemplos, mas mantém a criação inicial do administrador. Nunca use a credencial de demonstração em uma publicação real.

## Configuração inicial necessária para a demonstração

O prompt não informou preços de operação, limpeza, insumos nem duração de turno/expediente. Por isso:

- A migration fornece **4 horas por turno** e **08:00–20:00** apenas como parâmetros iniciais de demonstração, editáveis em Administração → Taxas e expediente. Confirme esses parâmetros com o Hub.
- Operação, limpeza e insumos começam **sem valor (NULL)**. A solicitação mostra uma estimativa preliminar clara. A aprovação só ocorre quando todos os valores aplicáveis estiverem configurados. Para uma demonstração sem cobrança dessas taxas, informe explicitamente **0** nos três campos; isso é uma decisão sua, não uma taxa inventada pelo software.
- Os valores de operação/limpeza configurados são **por reserva**, e insumos se aplicam a P2 acadêmico sem pagamento dentro da cota. A unidade deve ser confirmada com o Hub antes de uso real.
- Turnos são `arredondar para cima(duração / horasPorTurno)`. Para uma duração/ano sem preço cadastrado, o sistema impede orçamento e pede o cadastro. Não extrapola preços e não aplica automaticamente a tabela de 2026 a outro ano.
- Em Administração → Preços por ano, cadastre os percentuais **100/75/50** para cada quantidade de turnos necessária em novos anos.
- Feriados são exclusivamente os registros ativos do banco; os exemplos de 2026 não são um calendário oficial completo. Cadastre feriados locais e datas dos próximos anos.

## Fluxo para apresentar

1. Na agenda, navegue entre meses, selecione espaço e data. Consulte intervalos livres e ocupados.
2. Selecione início/fim, preencha solicitante e evento, persona, inscrição paga, financiamento e atividade acadêmica quando P2.
3. Clique **Calcular e revisar**: o resumo apresenta duração, cota, taxação, valor, sinal e restante. Confirme.
4. Entre no CRM. A solicitação aparece no dashboard e em Reservas → Pendentes.
5. Confira a persona declarada. Abra detalhes, aprove, recuse com motivo ou altere data/horários/espaço/persona. Cada alteração recalcula todos os valores.
6. Consulte o perfil do empreendedor, suas reservas, atendimentos, visitas e histórico. Cadastros com CNPJ existente são vinculados sem sobrescrever dados a partir de um formulário público.
7. Atualize o calendário; ele também se atualiza a cada 30 segundos enquanto visível. Pendentes/aprovadas bloqueiam o intervalo. Recusadas/canceladas liberam.
8. Registre pagamentos manualmente. Não há gateway ou cobrança automática. Conclua após o término do evento.

Dados fictícios iniciais: três espaços, cinco empreendedores com CNPJs de teste, seis personas, preços, feriados, atendimento, visita e duas reservas de exemplo (quando a execução ocorre em 2026). Uma reserva pendente externa demonstra a estimativa preliminar; a aprovada é da persona P1 (isenta). Ambas usam o mesmo serviço de cálculo. Uma nova solicitação permite demonstrar o fluxo completo.

## Variáveis

| Variável | Uso |
|---|---|
| DB_URL | URL JDBC MySQL; exemplo já está em `.env.example`; não se aplica ao perfil local |
| DB_USERNAME / DB_PASSWORD | Usuário e senha local do banco |
| MAIL_USERNAME / MAIL_PASSWORD | Remetente SMTP e senha de app; opcionais |
| ADMIN_LOGIN / ADMIN_PASSWORD | Credencial inicial de desenvolvimento |
| SEED_DEMO | `true` ou `false`; dados fictícios em banco novo |
| PORT | Porta HTTP, padrão 8080 |
| LOCAL_DB_PASSWORD | Senha opcional do H2; configure antes de criar o banco |

`.env` e `.env.local` estão no `.gitignore` e não acompanham o ZIP. No modo local, edite `.env.local`; no MySQL, edite `.env`. Senhas podem ser definidas também como variáveis reais de ambiente; têm precedência sobre o arquivo. Não coloque credenciais em `application.properties`. Arquivo `.env` é tratado como Java properties: use valores sem aspas; uma barra invertida literal deve ser escapada como `\\`.

## Email Gmail

Opcional. Na conta que enviará emails, habilite a verificação em duas etapas e, se a conta permitir, gere uma **senha de app**. Não utilize a senha normal do Gmail.

```properties
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=cole_a_senha_de_app_localmente
```

SMTP configurado em `smtp.gmail.com:587`, STARTTLS e autenticação. Reinicie o servidor após alterar as variáveis. Recebimento, aprovação, recusa, alteração e cancelamento enviam HTML simples **após o commit**. Sem credenciais, o processo registra no log que envio não está configurado e continua. Uma falha SMTP não desfaz a reserva: consulte o log. O MVP não implementa fila/reenvio automático. Emails só são efetivamente enviados quando credenciais válidas e acesso SMTP estão disponíveis.

## Regras e integridade

- Sobreposição: `novoInicio < existenteFim && novoFim > existenteInicio`. Intervalos adjacentes são permitidos.
- Valida horário, passado, expediente, espaço ativo/disponível e capacidade no servidor, incluindo aprovação/alteração.
- CNPJ é textual, normalizado na tela a 14 dígitos e UNIQUE no banco. A validação é de formato; os CNPJs fictícios de demonstração não representam empresas reais.
- Cota é compartilhada **por persona e mês da reserva**, não por empreendedor; antecedência ≥15 dias contada desde a solicitação original. P1/P6 não têm cotas. P2 sem pagamento deve ser declarado acadêmico, pois é o cenário definido no prompt.
- Uma reserva usa a cota somente quando há saldo para **toda** sua duração; o prompt não define fracionamento de uma mesma reserva entre preços. Fora da cota, a solicitação continua possível a 100%.
- Pendente aloca horas provisórias; aprovação transfere para utilizadas; recusa libera. Na aprovação, a reserva tem crédito pelas próprias horas já alocadas, preservando ordem de chegada.
- Cancelamento com antecedência ≥30 dias libera sem penalidade. Com menos de 30 dias, reserva que usou cota gera débito de **2× duração**, em substituição à alocação anterior. A penalidade é lançada na **cota do mês em que se cancela** (cota atual), e o excedente segue pelos meses seguintes. Horas livres não acumulam.
- Força maior/exceção exige ADMIN e justificativa. Cancelamentos/reclassificações ficam no histórico. Não há multas monetárias, reembolsos ou outras regras financeiras inventadas.
- Alterar reserva com pagamento registrado exige regularizar previamente o registro manual, para não recalcular silenciosamente valores já pagos.
- Valores monetários usam BigDecimal e duas casas, HALF_UP. Sinal é calculado após a taxação: 30% ou 60% em domingo/feriado. Reserva de valor zero mantém percentual e sinal calculado zero.
- Escritas de reserva, cota e cadastros usam uma **linha de configuração bloqueada com `SELECT FOR UPDATE`** na mesma transação. Isso serializa concorrência no MVP, evita duplicar CNPJ/horário e ordena a alocação da cota também entre diferentes espaços e processos. Uma transação que falha é revertida inteira. Não usa lock somente em memória.
- ADMIN gerencia parâmetros, contas e exceções; COLABORADOR gerencia operação/CRM. CSRF permanece habilitado. JPA usa parâmetros. Saídas HTML são escapadas. A agenda pública não expõe contatos/CNPJ; pendentes são exibidas como “Solicitação em análise”.

## Arquitetura e modelo físico

`controller` cuida de HTTP e views; `service` concentra regras, transações e validações; `repository` acessa JPA; `entity` mapeia o modelo; `dto` transporta formulário/orçamento; `security` autentica; `config` inicializa relógio/dados; `exception` traduz erros.

| Tabela | PK / relacionamentos e diferenças do modelo original |
|---|---|
| empreendedor | `id_empreendedor` numérico; CNPJ CHAR/VARCHAR UNIQUE deixa de ser PK; telefone textual; status representado por `ativo` |
| espaco | `id_espaco`; capacidade, disponibilidade e ativo; **sem FK para reserva** |
| colaborador | `id_colaborador`; login UNIQUE, senha BCrypt, perfil ADMIN/COLABORADOR e ativo |
| reserva | `id_reserva`; FKs `id_empreendedor`, `id_espaco`, `id_colaborador_responsavel` nullable e `persona_id`; início/fim substituem hora; campos financeiros e status separados, título/características do evento e horas alocadas preservam o orçamento |
| atendimento | `id_atendimento`; FKs para empreendedor/colaborador e campos originais |
| visita | `id_visita`; FKs para empreendedor/colaborador e campos originais; status AGENDADA/REALIZADA/CANCELADA |
| persona | `id`; códigos P1–P6 UNIQUE, descrições e cota configurável |
| cota_mensal | `id`; FK persona; UNIQUE persona/ano/mês; totais, utilizadas, pendentes e dívida |
| movimentacao_cota | `id`; FKs persona/reserva; quantidade assinada, tipo, instante, descrição e **mes_referencia** para representar competência e propagação de dívida |
| feriado | `id`; data UNIQUE, descrição e ativo |
| tabela_preco | `id`; UNIQUE ano/turnos/percentual, valor e ativo |
| configuracao | uma linha `id=1`; expediente, duração de turno e taxas que não foram informadas |
| historico_reserva | `id`; FKs reserva/colaborador; ação, instante, descrição; necessário para registrar alterações e compor histórico CRM |

As cardinalidades originais são preservadas. Campos Java usam `id`, mapeados às PKs físicas acima. FKs, UNIQUE, CHECK e índices estão em `src/main/resources/db/migration/V1__modelo_fisico.sql`. `V2` cadastra regras/preços iniciais. O administrador e exemplos são criados pelo initializer, equivalente ao seed de desenvolvimento com BCrypt, sem senha em texto puro no banco. Flyway controla versão/checksum; não altere migrations já aplicadas: adicione uma nova.

## Estrutura

```text
raiz-do-projeto/
  HANDOVER.md
  pom.xml
  .env.example
  .vscode/
  sql/criar-banco.sql
  src/main/java/br/com/inpetu/hub/
    config/ controller/ dto/ entity/ enums/ exception/ repository/ security/ service/
  src/main/resources/
    application.properties
    db/migration/
    templates/public/ auth/ admin/ error/
    static/css/ js/
  src/test/
  docs/
  configurar.sh configurar.ps1 configurar.bat
  iniciar.sh iniciar.ps1 iniciar.bat
  maven.sh maven.ps1 maven.bat
  scripts/runtime.sh scripts/runtime.ps1
  .env.local.example
```

## Testes e empacotamento

```sh
# macOS/Linux
bash maven.sh test
bash maven.sh clean package
```

```powershell
# Windows
.\maven.bat test
.\maven.bat clean package
```

Os launchers acima funcionam sem precisar de Maven no PATH. Os testes usam H2 **em memória**, com modo MySQL e migrations Flyway; o perfil local usa H2 **em arquivo**, e o perfil padrão usa MySQL. Os testes não precisam de Gmail ou do seu banco local, nem leem seu `.env`. Cobrem conflitos, sinal, feriados/domingo, cotas, aprovar/recusar/alterar/cancelar, validação, login, CSRF e permissões. Consulte `docs/VALIDACAO.md` para a evidência realizada nesta entrega.

## Solução de problemas

| Mensagem | Correção |
|---|---|
| Arquivo ou diretório inexistente ao chamar um script | Abra o terminal na pasta que contém `pom.xml` e `configurar.sh`; veja o handover |
| `java`/`mvn` não reconhecido | Use os scripts `configurar`, `iniciar` e `maven`; eles não dependem do PATH global |
| Download falhou | Confira acesso a api.adoptium.net, GitHub e repo.maven.apache.org; repita configurar |
| Banco H2 já está em uso | Pare a outra execução antes de iniciar novamente |
| Access denied / Communications link failure | Inicie MySQL; confira porta, usuário, senha e banco em `.env` |
| Porta 8080 ocupada | Pare o processo anterior ou adicione `PORT=8081` ao arquivo de configuração |
| Tabela de preços não configurada | Cadastre ano/turnos/percentuais no painel ADMIN |
| Taxas não configuradas | Informe valores/zero explícito em Administração → Taxas e expediente |
| Login inválido após mudar a configuração | A configuração não redefine senha de conta existente; edite no painel |
| Migration checksum mismatch | Restaure o arquivo original e crie uma nova migration para alterações |
| Email não enviado | Confira log, senha de app, STARTTLS e restrições da conta |
| Botão mostra acesso restrito | Confira perfil, sessão e CSRF; faça login novamente |

## Referências técnicas

- [Spring Boot — requisitos](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [MySQL — instalação oficial](https://dev.mysql.com/doc/refman/8.0/en/installing.html)
- [Google — senhas de app](https://support.google.com/accounts/answer/185833?hl=pt-BR)

A identidade visual foi implementada a partir do print fornecido: cabeçalho branco, marca textual, painel azul à esquerda, calendário claro à direita, bordas arredondadas e espaçamento amplo. O calendário reflete os dados reais do MVP, não os eventos fictícios do print.
