# Testar a entrega 0.4.3

## Conferência da equipe pelo executável

1. Baixe o ZIP completo correto para o sistema/arquitetura, extraia e abra o iniciador num computador sem Java ou VS Code preparados.
2. Aguarde Pronto, clique Iniciar e teste os cinco acessos de [ACESSOS.md](ACESSOS.md). Só Bernardo deve acessar Configuração. Confira também Mostrar senha.
3. Num banco novo, confirme **Sem espaços** na agenda. Cadastre um espaço ativo e disponível em Cadastros → Espaços. Sem reservas, ele deve aparecer livre. Confira reservas parciais, dia totalmente ocupado e cancelamento.
4. Pare e inicie novamente: cadastros, reservas e acessos devem permanecer.
5. Feche só a aba e reabra o executável: a instância existente deve reaparecer. Use **Fechar iniciador** para encerrar de verdade e confira `logs/sessoes/.../log.txt`, com as versões em cada evento.
6. Depois da primeira configuração, teste o pacote instalado sem internet. Relatórios devem permanecer pendentes, sem falsa confirmação de envio.
7. Numa cópia de teste da 0.4.2, confira a atualização para 0.4.3 e a preservação de banco, chave e contas. A família 0.5 não deve ser instalada automaticamente.

Registre sistema, arquitetura, versão, passos, esperado e observado ao relatar um bug. Revise os logs; não compartilhe banco, chaves ou backups.

## Validação automática do código

A CI executa cinco testes da agenda em Node, sete testes Node do receptor/coletor, testes Go do iniciador e testes Java do aplicativo. O roteiro Java valida HTTP, segurança, perfis, contas, persistência e backup com dados descartáveis. Os números são a referência desta reorganização; confira Actions para o resultado efetivo.

Depois, o smoke executa o próprio binário com seu Java privado: instalação, quinze logins, agenda vazia/livre, cadastro, reinício, troca real do iniciador e preservação de banco/chave. A sessão deve continuar única durante a troca, registrar as versões antiga/nova e conter eventos HTTP/auditoria sem senhas. As simulações usam rede local e não publicam releases falsas.

A matriz cobre Ubuntu 24.04 x64/ARM64, macOS 15 Intel/Apple Silicon, Windows Server 2025 x64 e Windows 11 ARM64. Esses testes não comprovam todas as distribuições/edições nem substituem conferência visual de navegador, SmartScreen e Gatekeeper.

Comandos de desenvolvimento e ferramentas necessárias estão em [ESTRUTURA.md](ESTRUTURA.md). O usuário final usa somente o executável.
