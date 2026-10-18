# Acompanhamento do relatório de bugs

Fonte atual: `caçabugs_0.1_Joaoe.docx`, recebido em 18/10/2026, com 12 capturas. O conteúdo corresponde aos sintomas antes acompanhados pelo PDF. O responsável autorizou **todos os itens restantes** nesta entrega. Documento original mantido fora do Git.

| Parte | Estado da entrega 0.4.7 |
| --- | --- |
| Espaços/capacidade | Correção 0.4.4 preservada; regressões mantidas. |
| Nome/email/telefone | Corrigidos no formulário público e cadastro interno, com rejeição amigável e correção/reenvio. |
| Datas e erros | ISO, preservação da seleção, edição do rascunho, limites até 31/12 do próximo ano e mensagens sem detalhes técnicos. |
| Confirmação | Campo de código aparece somente para solicitação com código ativo; modo local explica conferência manual. |
| Agenda e clareza | Consulta por dia, filtro com capacidades, rótulos/cores de disponibilidade, grade estreita e asteriscos nos obrigatórios. |

Causas, arquivos, evidências e limites em [CORRECOES-RELATORIO.md](CORRECOES-RELATORIO.md). Publicada na [0.4.7](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.7) com os 11 jobs da [CI](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39152880226) aprovados, incluindo seis alvos nativos. Não significa ativação de SMTP/Render nem confirmação da existência de emails/telefones.

## Histórico das entregas anteriores

## Escopo da parte 1

Somente o cadastro de espaços recebe a nova recuperação da validação. A capacidade continua inteira no banco e agora possui min=1, step=1 e limite compatível com inteiro de 32 bits na tela. O servidor rejeita zero, negativos, frações, letras e estouro numérico. Valores financeiros mantêm sua precisão decimal.

Após falha de regra, a transação é revertida e a tela é renderizada novamente com HTTP 400, mensagem simples e campos preservados; não há retorno à página genérica de erro. Ao voltar pelo cache do navegador, o JavaScript reabilita o envio somente no formulário de espaços. O log registra a rejeição de servidor sem formulário ou mensagem livre. Tentativas barradas pelo navegador não chegam ao log do servidor.

Sem migrations, reset do banco, mudança de contas ou ativação do receptor. Os itens das próximas partes continuam pendentes. Testes manuais: TESTAR.md. Evidências da entrega: VALIDACAO.md e Actions da tag.

## Atualização de logs na 0.4.5

O novo pedido priorizou logs antes das demais partes. Mensagens técnicas de conversão na reserva foram substituídas por orientação simples; a validação, recusas tratadas e falhas inesperadas ganham diagnóstico sanitizado. Isso atende a parcela de mensagem/log da página 2, mas não conclui a parte de datas: janela de anos, preservação e preenchimento agenda → reserva continuam pendentes. Também não conclui os demais itens do PDF. Envio real ao GitHub ainda aguarda configuração do receptor.
