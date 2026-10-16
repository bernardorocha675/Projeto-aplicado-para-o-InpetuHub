# Como consultar os logs — versão 0.3

Log é um registro de acontecimentos do programa. Nesta versão ele continua no terminal e também é salvo em arquivo, para permitir investigação depois de fechar o programa.

## Onde encontrar

Inicie normalmente e aguarde `event=SERVER_READY`. No VS Code, abra `logs/inpetu.log` dentro da pasta versão 0.3. Essa pasta aparece após iniciar o servidor, não precisa criá-la à mão.

Para acompanhar enquanto o programa roda, abra **outro terminal** na pasta versão 0.3:

Linux/macOS:

```sh
tail -f logs/inpetu.log
```

Windows PowerShell:

```powershell
Get-Content .\logs\inpetu.log -Tail 50 -Wait
```

Ctrl+C nesse segundo terminal para parar apenas a leitura. No terminal do servidor, Ctrl+C encerra o programa.

## Como ler uma linha

Exemplo fictício:

```json
{"time":"2026-10-15T18:00:00Z","level":"WARN","logger":"br.com.inpetu.hub.observability.OperationalEvents","requestId":"2898377e-3348-4798-83d4-99469b252362","message":"event=HTTP method=POST status=403 durationMs=12"}
```

| Campo | Significado |
|---|---|
| time | Data e hora UTC; no horário de Brasília, normalmente subtraia 3 horas. |
| level | INFO: evento normal. WARN: recusa ou lentidão. ERROR: erro de servidor. Um WARN não significa necessariamente bug. |
| requestId | Identificador criado pelo servidor para relacionar os eventos da mesma requisição. `-` significa evento fora de uma requisição. |
| message | Evento e seus campos controlados; não contém o formulário enviado. |
| exceptionType / frames | Quando há exceção: classe do erro e pontos do código para um desenvolvedor investigar, sem o texto da exceção. |

`status=200` indica resposta HTTP concluída, mas não comprova que uma operação foi salva. `302` costuma ser um redirecionamento normal. `403` indica acesso recusado; `429`, excesso de tentativas; `500` ou maior, erro do servidor. `durationMs=1000` equivale a um segundo. Requisições de pelo menos um segundo são WARN.

Eventos `AUDIT` espelham ações confirmadas no banco com identificação interna do responsável, ação, alvo e resultado. Não incluem o nome/login nem o campo livre de detalhes. Consulte também a auditoria completa na área administrativa: http://localhost:8080/admin/config/auditoria (exige perfil de administrador).

## Investigar e relatar um problema

1. Anote o horário aproximado e a ação que tentou fazer, sem incluir senha.
2. No navegador, pressione F12, abra a aba Rede/Network, repita a ação e selecione a requisição que falhou.
3. Em cabeçalhos de resposta/Response Headers, copie apenas `X-Request-ID`. Não copie Cookie, Authorization, dados enviados ou a resposta inteira.
4. Procure esse identificador em `logs/inpetu.log` pelo Ctrl+F do VS Code. Se houve rotação, extraia o arquivo `.gz` do horário correspondente em uma pasta privada e pesquise nele.
5. Envie ao responsável somente as linhas necessárias, revisadas, o horário, os passos, o esperado e o que aconteceu. Nunca envie `.secrets`, `.env.local`, o banco ou o ZIP privado de acessos.

Se a falha acontecer antes de o servidor iniciar, pode não existir requestId. Copie o comando e o tipo de erro do terminal, revisando o conteúdo antes de compartilhar.

## Espaço e privacidade

A cada dia UTC ou ao chegar aproximadamente a 10 MB, o programa arquiva o log em `logs/inpetu.AAAA-MM-DD.N.log.gz`. Mantém até 14 dias e aplica um limite de 200 MB aos arquivos antigos. A limpeza ocorre na inicialização e a cada minuto, preserva arquivos modificados há menos de um minuto para não interromper a compactação e mantém as 14 datas UTC mais recentes (incluindo hoje). O arquivo ativo e arquivos ainda recentes ficam fora desse limite temporariamente: monitore o disco.

Por padrão, mensagens livres de bibliotecas aparecem como `FRAMEWORK_EVENT`, com o nome do logger e o tipo/pontos da exceção quando houver. Isso evita que uma mensagem SQL/SMTP acabe gravando senha, email ou dados de formulário. O log HTTP não grava endereço da página, parâmetros, corpo, IP, cookies, cabeçalhos recebidos, senha ou chave privada. O identificador recebido do cliente é descartado e substituído por um UUID novo.

Os arquivos são locais, ficam fora do Git e não são acessíveis por uma rota do site. Permissões são restritas ao usuário do computador; administradores do sistema ainda podem acessá-los. Não são criptografados nem imunes a alterações. O backup cifrado do sistema inclui a auditoria do banco, mas não esses arquivos técnicos. Não troque a configuração de logs ou ative depuração sem revisão.

## Testar

Com o servidor parado: `bash testar.sh` no Linux/macOS ou `.\testar.bat` no Windows. Abra `relatorios-testes/ultimo.html`. O comando testa também persistência do log, correlação, exclusão de dados sintéticos privados, rotação e transações. Para testes manuais, gere um acesso normal, uma recusa de permissão e um login incorreto; confira os eventos sem provocar perda de dados ou compartilhar os logs completos.
