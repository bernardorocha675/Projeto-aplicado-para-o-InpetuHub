# Guia de uso

Vale para a linha 1.0. **Salvar relatório** e o registro de cliques chegam na alpha.2. Para baixar, abrir e encerrar, veja o [README](../README.md#como-executar).

## Acessos de demonstração

Estas contas são **públicas**, só para testes no computador. Não use dados reais nem repita estas senhas em outro lugar.

| Pessoa | Usuário | Senha | Perfil |
| --- | --- | --- | --- |
| Bernardo | `bernardo` | `bernardo040` | Administrador |
| Milena | `milena` | `milena040` | Colaborador |
| João F | `joaof` | `joaof040` | Colaborador |
| João E | `joaoe` | `joaoe040` | Colaborador |
| Guilherme | `guilherme` | `guilherme040` | Colaborador |

O usuário aceita maiúsculas, acentos e espaços; a senha precisa ser exata. Uma senha trocada volta ao padrão quando o sistema reinicia. A sessão termina após 20 minutos sem uso ou 2 horas no total.

## Da reserva à aprovação

1. Na **Agenda**, escolha um dia e um horário livre.
2. Preencha o pedido, confira o resumo e confirme. O pedido **não ocupa** o horário e vence em 24 horas.
3. Na **Área Interna**, abra **Solicitações recebidas**, confira a identidade e encaminhe, ou recuse.
4. Abra a reserva pendente e aprove. Ela também vence em 24 horas. **Só a reserva aprovada ocupa a agenda.**

Excluir um espaço, recusar um pedido e cancelar uma reserva pedem confirmação. Um espaço com reservas não pode ser excluído: desative-o.

**Limite desta versão:** só as reservas da persona P1 podem ser aprovadas, porque ainda não há tela para configurar taxas e preços.

## Onde ficam os dados

| Sistema | Pasta |
| --- | --- |
| Windows | `%LOCALAPPDATA%\InPETU-Hub\1` |
| Linux | `~/.config/InPETU-Hub/1` |

| Dentro dela | O que é |
| --- | --- |
| `dados/` | O banco (`inpetu.mv.db`) |
| `backups/` | As 10 cópias mais novas do banco, feitas antes de cada abertura |
| `logs/` | Os registros de funcionamento |
| `versoes/` | Até 3 versões instaladas |

Nada é enviado para a internet, a não ser a consulta de versões novas ao GitHub. A pasta `InPETU-Hub/0.4`, da versão antiga, não é usada.

### Voltar a um backup

1. Feche o iniciador e espere a janela sumir.
2. Guarde uma cópia do `dados/inpetu.mv.db` em outra pasta.
3. Copie o backup escolhido para `dados/` com o nome `inpetu.mv.db`.
4. Abra o iniciador. O que foi feito depois daquele backup não aparece mais.

## Logs e privacidade

- `logs/iniciador.log`: abertura, atualização e encerramento.
- `logs/sistema.log`: o que o sistema fez, com rota, resultado e tempo.

O registro começa sozinho ao abrir. Ele mostra as telas abertas, as ações, os cliques na agenda, as confirmações, os **nomes** dos campos recusados e os erros.

Os logs foram feitos para não guardar senhas, dados de formulário nem a pasta da pessoa. Os horários estão em UTC: para Brasília, subtraia 3 horas.

**Nunca compartilhe** a pasta `dados/` ou `backups/`: elas têm os dados cadastrados.

## Problemas comuns

| O que aparece | O que fazer |
| --- | --- |
| “O iniciador já está aberto” | Use a janela que já está aberta |
| “O banco está em uso” | Feche a outra cópia do sistema |
| “Sem permissão para usar a pasta de dados” | Confira a permissão da pasta acima |
| “O pacote do sistema está incompleto” | Baixe o ZIP de novo e extraia inteiro |
| “Sem conexão” ou “O GitHub limitou as consultas” | Nada: o sistema usa a versão instalada |
| A página não abre | Na janela, clique em **Abrir navegador** |

## Relatar um problema

Informe a versão, o sistema (Windows ou Linux), o horário, o que você fez, o que esperava e o que aconteceu. Na janela do iniciador, clique em **Salvar relatório**, escolha a pasta, confira o arquivo e mande para a equipe. Ele junta as 5 últimas sessões e nada é enviado sozinho. Na alpha.1, anexe só o trecho do log daquele horário. Falhas de segurança seguem a [política de segurança](../.github/SECURITY.md).
