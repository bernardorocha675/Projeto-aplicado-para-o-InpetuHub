# Testar a entrega

## Conferência da equipe pelo executável

1. Baixe o ZIP completo correto para o sistema/arquitetura, extraia e abra o iniciador num computador sem Java ou VS Code preparados.
2. Aguarde Pronto, clique Iniciar e teste os cinco acessos de [ACESSOS.md](ACESSOS.md). Só Bernardo deve acessar Configuração. Confira também Mostrar senha.
3. Num banco novo, confirme **Sem espaços** na agenda. Cadastre um espaço ativo e disponível em Cadastros → Espaços. Sem reservas, ele deve aparecer livre. Confira reservas parciais, dia totalmente ocupado e cancelamento.
4. Pare e inicie novamente: cadastros, reservas e acessos devem permanecer.
5. Feche só a aba e reabra o executável: a instância existente deve reaparecer. Use **Fechar iniciador** para encerrar de verdade e confira `logs/sessoes/.../log.txt`, com as versões em cada evento.
6. Depois da primeira configuração, teste o pacote instalado sem internet. Relatórios devem permanecer pendentes, sem falsa confirmação de envio.
7. Numa cópia de teste da versão anterior, confira a atualização para a versão atual e a preservação de banco, chave e contas. A família 0.5 não deve ser instalada automaticamente.

Registre sistema, arquitetura, versão, passos, esperado e observado ao relatar um bug. Revise os logs; não compartilhe banco, chaves ou backups.

## Validação automática do código

A CI executa testes Node das interfaces da agenda/cadastros e do receptor/coletor, testes Go do iniciador e testes Java do aplicativo. O roteiro Java valida HTTP, segurança, perfis, contas, persistência e backup com dados descartáveis. Os números são a referência desta reorganização; confira Actions para o resultado efetivo.

Depois, o smoke executa o próprio binário com seu Java privado: instalação, quinze logins, agenda vazia/livre, cadastro, reinício, troca real do iniciador e preservação de banco/chave. A sessão deve continuar única durante a troca, registrar as versões antiga/nova e conter eventos HTTP/auditoria sem senhas. As simulações usam rede local e não publicam releases falsas.

A matriz cobre Ubuntu 24.04 x64/ARM64, macOS 15 Intel/Apple Silicon, Windows Server 2025 x64 e Windows 11 ARM64. Esses testes não comprovam todas as distribuições/edições nem substituem conferência visual de navegador, SmartScreen e Gatekeeper.

Comandos de desenvolvimento e ferramentas necessárias estão em [ESTRUTURA.md](ESTRUTURA.md). O usuário final usa somente o executável.

## Cadastro de espaços — fix 0.4.4

1. Entre na área interna e abra Espaços → Novo cadastro.
2. Informe nome, capacidade 0 e tente salvar. O navegador deve orientar que o mínimo é 1. Tente também -1 e 1,5: capacidade precisa ser inteira e positiva.
3. Corrija para 25 e salve. O cadastro deve aparecer na lista, sem precisar reabrir a tela.
4. Edite o espaço, altere o nome/capacidade e salve. Confira que alterou somente esse registro.
5. Para simular validação no servidor, o teste automatizado envia diretamente dados inválidos: a mesma tela retorna com mensagem simples, valores e opções preservados. Corrigir e reenviar deve funcionar; o banco mantém a versão anterior até salvar dados válidos.
6. Confira o registro CADASTRO_INVALIDO/NEGADO nos logs locais da sessão para rejeições do servidor. Uma tentativa bloqueada apenas pelo navegador não chega ao servidor.

## Logs — fix 0.4.5

1. Abra o executável, inicie o aplicativo e faça login. As ações já devem estar sendo registradas, sem iniciar teste manual.
2. Envie uma reserva de teste com campos obrigatórios em branco ou data inválida. O navegador pode impedir o envio; se o pedido chegar ao servidor, ele deve registrar VALIDATION_REJECTED com HTTP 400 e mostrar orientação simples.
3. Uma recusa de regra gera REQUEST_REJECTED (WARN); uma falha inesperada gera REQUEST_FAILED (ERROR) com tipo de erro e pontos do código no log. A tela não deve exibir detalhes técnicos; a referência para suporte coincide com X-Request-ID e com o log.
4. Feche pelo botão Fechar iniciador. Confira log.txt e relatorio.json em logs/sessoes. Ambos precisam ter as versões e os eventos HTTP/AUDIT/validação. Não confundir CONCLUIDO com ausência de erros.
5. Abra novamente após uma sessão interrompida. A sessão anterior deve ser recuperada sem apagar dados. Falhas de gravação são simuladas pelos testes automáticos em uma pasta descartável; não altere permissões nem encha o disco da instalação real para testar.
6. Quando o receptor estiver ativo, uma resposta de fila deve manter o envio pendente. Somente o recibo final do GitHub cria enviado.json. Sem receptor configurado, deve aparecer explicitamente que o envio aguarda configuração, e os registros ficam locais.

Falhas de SMTP são testadas com serviço simulado, sem enviar emails reais. Não publique senhas, formulários ou logs operacionais brutos.

## Relatório caçabugs — 0.4.10

1. Selecione um dia na agenda, confira nomes/capacidades e escolha um horário. A reserva deve trazer data ISO e espaço/horários preenchidos.
2. Tente nome `João 123`, email `11231231@a` e telefone com letras: devem ser rejeitados. Corrija para nome com acentos/hífen/apóstrofo, email completo e telefone com DDD; números do evento/empresa continuam permitidos.
3. Informe hora final anterior à inicial e envie: a mensagem deve ser simples e a data/contatos devem permanecer. Corrija e revise; use Voltar para editar, altere outro campo e confira que data e horários permanecem.
4. Confira mínimo hoje e máximo 31/12 do próximo ano. Uma data do ano posterior deve ser recusada também pelo servidor. Preços do ano escolhido precisam estar cadastrados pelo gestor; não são inventados automaticamente.
5. Sem SMTP, confirme a solicitação: a tela deve explicar conferência manual e não pedir código. Isso não aprova nem bloqueia horário. Com SMTP configurado, código ativo deve manter oito dígitos, expiração e limite de tentativas; teste SMTP real exige configuração separada.
6. Todos os campos obrigatórios devem ter `*`. Confira agenda com dia livre/parcial/ocupado e em janela estreita; os sete dias da semana devem ficar visíveis.

Evidências e limites: [CORRECOES-RELATORIO.md](CORRECOES-RELATORIO.md).

## Abertura e atualização — fix 0.4.11

1. Com o sistema em execução, encerre o iniciador à força (Gerenciador de Tarefas, Monitor de Atividade ou `kill`), sem usar Parar. Abra o executável de novo e clique Iniciar: a tela deve indicar a recuperação e, em até cerca de 30 segundos, abrir o sistema com os dados preservados.
2. Numa cópia descartável com duas versões instaladas, altere um arquivo da pasta da versão mais nova. Ao abrir, a versão anterior deve continuar utilizável e a pasta alterada deve ser preservada como `installed/.danificada-…`.
3. Na área interna, tente cancelar uma reserva aprovada cujo horário já terminou: o sistema deve recusar e orientar Concluir. A exceção de força maior, com justificativa, continua permitida ao gestor.
4. Confira em `logs/sessoes/.../log.txt` os códigos ORPHAN_APP_FOUND/ORPHAN_APP_STOPPED e, nas falhas, o campo `result` de ACTION_FAILED.

O smoke da CI executa automaticamente o item 1 com pasta acentuada e, no Linux, sem locale configurado.

## Download, avisos e endereço local — fix 0.4.12

1. Numa conexão lenta ou limitada, confira que o download da atualização mostra o progresso em MB e termina, mesmo levando mais de 5 minutos.
2. Abra o executável enquanto outra janela do iniciador estiver aberta sem `running.json` (ou com a pasta de dados sem permissão): deve aparecer uma janela de aviso, em vez de nada acontecer.
3. Com o sistema aberto, um endereço diferente de 127.0.0.1/localhost na barra do navegador deve ser recusado com “Endereço recusado”.

O smoke da CI confere automaticamente a recusa do host externo.
