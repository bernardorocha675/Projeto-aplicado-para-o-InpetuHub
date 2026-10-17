# Android — versão experimental independente

A linha Android tem versão própria: **0.1.0-experimental.1**. A estável de computadores permanece **0.4.5**. O usuário escolheu explicitamente que o celular funcionasse sozinho; por isso é um port nativo com SQLite, e não uma janela que dependa de servidor.

## Instalação para a equipe

1. No celular Android 8.0 ou superior, abra a release **InPETU Hub Android — EXPERIMENTAL** e baixe o arquivo que termina em `.apk`.
2. Abra o arquivo baixado. Se o Android perguntar, permita instalar aplicativos por esse navegador ou gerenciador de arquivos. A permissão pode ser retirada após a instalação. A política de um celular corporativo pode impedir instalação fora da loja.
3. Toque em **Instalar** e abra **InPETU Experimental**. A marca experimental aparece no nome e em todas as telas.
4. Entre com um dos acessos de demonstração abaixo. Não precisa iniciar servidor nem instalar Java. A primeira abertura prepara o banco local.
5. Em **Espaços**, cadastre nome e capacidade inteira positiva; deixe Ativo e Disponível marcados. A agenda começa vazia.
6. O administrador deve abrir **Mais → Configurar expediente e taxas** e informar os valores aprovados. Se não há cobrança de uma taxa, digite `0,00` explicitamente. Não substituímos taxa indefinida por um zero presumido na aprovação.
7. Na **Agenda**, selecione a data e **Nova reserva**. Preencha os campos com `*`. O espaço mostra a capacidade. A solicitação fica pendente e não bloqueia a agenda.
8. Abra o detalhe, confira o vínculo com a instituição e aprove. O aplicativo verifica novamente capacidade, expediente, conflitos, valores e cota. Valores registrados podem ser revistos na aprovação, conforme saldo e configuração.
9. Confira pagamentos manualmente. Cancelamento de reserva com cota aprovada a menos de 30 dias pode gerar penalidade de duas vezes as horas. Força maior exige o administrador e justificativa.
10. Em **Mais**, exporte um backup privado antes de qualquer reinstalação ou manutenção. Desinstalar ou limpar os dados pelo Android apaga o banco do aplicativo.

| Login | Senha de demonstração | Perfil |
| --- | --- | --- |
| milena | milena040 | Colaborador |
| bernardo | bernardo040 | Administrador |
| joaof | joaof040 | Colaborador |
| joaoe | joaoe040 | Colaborador |
| guilherme | guilherme040 | Colaborador |

As senhas são públicas para demonstração e armazenadas no banco com hash PBKDF2/sal individual. Esta instalação não deve guardar dados confidenciais. As contas são criadas na primeira instalação; não há troca de senha nesta primeira versão.

## Dados, backup e logs

O SQLite fica na área privada do aplicativo. Não há permissão de rede, sincronização, upload ou serviço rodando em outro dispositivo. Dois celulares têm bancos, cotas e agendas diferentes. Nenhum dado do computador é importado automaticamente.

**Exportar backup privado** gera JSON com dados de negócio, configurações e movimentos de cota. Não inclui contas/senhas nem logs. Esse arquivo contém nomes e contatos: guarde-o de forma privada e nunca envie ao repositório. A restauração aceita apenas o formato Android e uma instalação sem espaços/reservas; não sobrescreve a instalação atual.

**Logs locais → Exportar log sanitizado** gera texto com os últimos 5.000 eventos, versão, sessão, IDs e tipos de erro. Não inclui conteúdo dos formulários, emails ou senhas. Logs são gravados automaticamente em SQLite. O Android pode encerrar o processo sem chamar uma rotina de fechamento; os eventos já persistidos continuam disponíveis. Não há promessa de registro quando o próprio disco não permite gravar.

Os formulários ainda não salvos podem ser descartados ao recriar a atividade, por exemplo por rotação ou encerramento do processo. Registros salvos persistem após reabrir; entrar novamente é obrigatório. Exporte o backup antes de desinstalar; não desinstale para tentar corrigir um login.

## Diferenças e limites

| Área | Primeira versão Android |
| --- | --- |
| Agenda, espaços e reservas | Local; horários aprovados, pendentes, recusa, cancelamento, conclusão. |
| Valores/cotas | Port das regras principais do desktop 0.4.5, com conferência manual; mudanças no limite de cota recalculam o histórico usando o novo limite. |
| Preços | Referência inicial de 2026; gestor cadastra preços de outros anos. |
| CRM e edição de reservas | Ainda não incluídos. Cancelar/criar outra reserva tem os efeitos de cota previstos; não é edição transparente. |
| Email, links públicos, MFA | Não incluídos nesta demonstração local. |
| Logs ao GitHub e atualização automática | Não incluídos; exportação e instalação de novas versões são manuais. |
| Banco desktop | Não compatível para importação direta; nenhum arquivo H2 é alterado. |

Não é uma conversão automática do JAR desktop nem uma declaração de equivalência completa. O APK usa APIs Android e SQLite. Nenhuma tela promete compartilhar reservas entre aparelhos.

## Roteiro de teste humano

- Instalar, abrir e entrar com os cinco usuários; errar a senha e conferir a mensagem; usar Mostrar senha.
- Cadastrar espaço com capacidade 0, decimal e válida; corrigir sem perder os dados da tela; desativar/reativar espaço.
- Selecionar data na agenda e conferir o preenchimento da nova reserva; datas passadas, quantidade acima da capacidade e fim anterior ao início devem ser recusados.
- Configurar taxas e criar duas solicitações sobrepostas: ambas podem ficar pendentes, mas somente uma pode ser aprovada no mesmo espaço/intervalo. Horário adjacente deve funcionar.
- Testar P1/P2/P3/P6, saldo insuficiente, domingo/feriado, pagamentos e cancelamentos com/sem força maior; comparar os valores com a configuração e o desktop quando aplicável.
- Sair/reabrir e conferir a persistência; exportar backup, restaurar numa instalação vazia de teste e conferir reservas. Não limpar sua única cópia para testar.
- Exportar log e procurar versão/ações/erros; nomes, emails, senhas e campos preenchidos não devem estar nele.
- Testar em modo avião e com fontes maiores do Android; conferir se os botões e campos podem ser alcançados pela rolagem.

As evidências automáticas e limitações estão no [handover Android](HANDOVER-ANDROID.md). Não testar com dados confidenciais.
