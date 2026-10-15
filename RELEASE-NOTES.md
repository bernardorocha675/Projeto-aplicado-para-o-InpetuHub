# InPETU Hub 0.3.0

- Logs técnicos em arquivo, com rotação e limpeza automática.
- Conferência e recuperação local de acessos, com o servidor parado.
- Botão Mostrar/Ocultar senha nos formulários.
- Handover e guias de instalação, operação e testes revisados.
- Pastas renomeadas: `versão 01` → `versão 0.1`, `versão 02` → `versão 0.2` e `versão 03` → `versão 0.3`.

## Como começar

Baixe e extraia `inpetu-hub-v0.3.0.zip`. Abra a pasta **versão 0.3** no VS Code e siga `COMO-INICIAR.md`: execute `bash configurar.sh` e depois `bash iniciar.sh` em Linux/macOS; no Windows, execute `.\configurar.bat` e depois `.\iniciar.bat`.

As contas, senhas, chaves, configurações privadas e bancos de dados não fazem parte do pacote público. Instalações existentes devem preservar esses arquivos e seguir a migração descrita em `HANDOVER.md` antes de trocar de pasta. O nome técnico do banco local continua `inpetu-v02.mv.db` para manter a compatibilidade.

O pacote contém o código e os scripts de preparação. A primeira configuração precisa de conexão com a internet. O arquivo `SHA256SUMS.txt` permite conferir a integridade do ZIP.
