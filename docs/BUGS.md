# Acompanhamento do relatório de bugs

Fonte: caçabugs_0.1_Joaoe.docx.PDF, seis páginas, recebido em 17/10/2026. O PDF completo e suas imagens foram lidos. Cada parte será entregue em fix separado e encerrada antes da próxima autorização. O documento original não foi copiado para o repositório.

| Parte | Itens do documento | Análise e estado |
| --- | --- | --- |
| 1 — Espaços | Capacidade 0, decimais e impossibilidade de salvar após corrigir (p. 2) | Corrigido e publicado na [0.4.4](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.4); seis ambientes nativos aprovados. |
| 2 — Dados de contato | Nome com números, email sem domínio completo e telefone com letras (p. 1) | Pendente. ReservaForm exige nome/telefone, mas ainda não valida seus formatos. CRUD já tem validação de domínio no email; diferenciar os fluxos antes de mudar. Não é possível comprovar existência de uma caixa de email apenas pelo formato. |
| 3 — Datas e erros da reserva | Ano atual e próximo, mensagem técnica, perda da data, data da agenda preenchida na reserva (p. 1–3) | Pendente. Há limite mínimo no JavaScript, sem limite máximo. A captura da mensagem de conversão é da reserva, apesar de o texto dizer login. PublicController já recebe a data na URL: ainda conferir a renderização e a ida/volta do formulário. |
| 4 — Confirmação | Código de confirmação não encontrado (p. 4) | Pendente. Código só é gerado/enviado com email configurado; modo local prevê conferência manual, mas a tela ainda oferece código mesmo nesse caso. Corrigir a orientação/fluxo sem inventar código nem desativar controles de produção. |
| 5 — Agenda e clareza | Lista de espaços/capacidades por dia, visual livre/ocupado e asteriscos nos campos obrigatórios (p. 3, 5–6) | Pendente. Comparar com o comportamento atual e a referência visual antes de implementar. |

## Escopo da parte 1

Somente o cadastro de espaços recebe a nova recuperação da validação. A capacidade continua inteira no banco e agora possui min=1, step=1 e limite compatível com inteiro de 32 bits na tela. O servidor rejeita zero, negativos, frações, letras e estouro numérico. Valores financeiros mantêm sua precisão decimal.

Após falha de regra, a transação é revertida e a tela é renderizada novamente com HTTP 400, mensagem simples e campos preservados; não há retorno à página genérica de erro. Ao voltar pelo cache do navegador, o JavaScript reabilita o envio somente no formulário de espaços. O log registra a rejeição de servidor sem formulário ou mensagem livre. Tentativas barradas pelo navegador não chegam ao log do servidor.

Sem migrations, reset do banco, mudança de contas ou ativação do receptor. Os itens das próximas partes continuam pendentes. Testes manuais: TESTAR.md. Evidências da entrega: VALIDACAO.md e Actions da tag.

## Atualização de logs na 0.4.5

O novo pedido priorizou logs antes das demais partes. Mensagens técnicas de conversão na reserva foram substituídas por orientação simples; a validação, recusas tratadas e falhas inesperadas ganham diagnóstico sanitizado. Isso atende a parcela de mensagem/log da página 2, mas não conclui a parte de datas: janela de anos, preservação e preenchimento agenda → reserva continuam pendentes. Também não conclui os demais itens do PDF. Envio real ao GitHub ainda aguarda configuração do receptor.
