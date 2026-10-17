# Testar a versão 0.4

A CI executa os testes da interface da agenda em Node, os testes do iniciador Go, os testes Java do aplicativo e o roteiro HTTP existente. Depois compila o binário, cria o Java privado e roda o pacote real em um diretório temporário.

O smoke do executável instala sem Java externo nem arquivo de senhas, confere os cinco logins e perfis, confere a agenda sem espaços, cria um espaço e verifica que os dias ficam livres, para e inicia novamente e simula a troca real para o iniciador do fix seguinte. São quinze autenticações; cadastro, chave e banco precisam permanecer. Depois encerra os processos. Nenhuma conta real ou banco da equipe é utilizado. Os testes do atualizador usam releases fictícias em loopback para conferir família/paginação, SHA-256, extração segura, preservação de dados, snapshot, offline, origem/token e lock.

A matriz cobre Ubuntu 24.04 x64/ARM64, macOS 15 Apple Silicon/Intel, Windows Server 2025 x64 e Windows 11 ARM64. O resultado está em Actions, no commit da release. O teste automatizado não verifica visualmente SmartScreen, Gatekeeper nem o navegador padrão de todos os ambientes.

## Conferência da equipe

1. Baixe o pacote correto, extraia e abra o iniciador num computador sem Java/VS Code preparados.
2. Aguarde Pronto, clique Iniciar e use cada uma das cinco contas de ACESSOS.md. Só Bernardo deve acessar Configuração.
3. Num banco novo, confirme **Sem espaços** na agenda. Cadastre um espaço em Cadastros → Espaços, marcando Ativo e Disponível para reservas. Confirme **Livre** na agenda. Aprove uma reserva parcial e confira os horários restantes; um dia completamente reservado deve aparecer ocupado. Pare, inicie e confirme que os dados continuam salvos.
4. Feche só a aba; abra o executável e confirme que o iniciador existente reaparece. Depois use Fechar iniciador para encerrar de verdade.
5. Após configurar uma vez, desconecte da internet e confirme que o pacote instalado inicia.
6. Quando existir um fix posterior 0.4.x, confirme atualização e backup numa cópia de teste. Uma release 0.5 não deve ser instalada automaticamente.

Ao relatar um bug, informe sistema/arquitetura, versão, passos, esperado e observado. Revise os logs locais antes de compartilhar e não envie banco, chave ou backups.

Para desenvolvimento, com JDK/Maven/Go, execute `go test ./...` dentro de launcher e `bash testar.sh` ou `testar.bat` na pasta da versão. O usuário final usa somente o executável.

A 0.4.2 exige uma única sessão durante a troca de iniciador, versões antiga/nova nos metadados e eventos HTTP/auditoria do Java. Conferir também o relatório automático em logs/sessoes, sem as senhas. Testes Go cobrem interrupção, recibo, duplicação, campos privados e limites; os testes Node validam receptor, identidade e publicação.
