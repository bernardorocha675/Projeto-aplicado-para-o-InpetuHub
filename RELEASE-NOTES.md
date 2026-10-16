# InPETU Hub 0.3.1fix

- Removido o segundo fator do login, do menu e da recuperação administrativa.
- Administradores e colaboradores acessam com login e senha, inclusive contas que já tinham autenticador configurado.

As demais funções permanecem: perfis e permissões, proteção de sessão, limite de tentativas, CSRF, logs, backups e recuperação local de senhas. A remoção do segundo fator reduz a proteção das contas administrativas.

## Atualizar e iniciar

Pare o servidor e faça backup antes de atualizar o código da pasta **versão 0.3**. Preserve `data/`, `.env.local` e `.secrets/`, incluindo a chave privada existente: ela continua sendo usada em outras verificações do sistema. As senhas não são redefinidas por esta atualização. Não há nova migration de banco; dados antigos de segundo fator permanecem no banco para compatibilidade, mas não são utilizados no acesso desta release.

Baixe e extraia `inpetu-hub-v0.3.1fix.zip`. Siga `COMO-INICIAR.md`: em Linux/macOS, execute `bash configurar.sh` e depois `bash iniciar.sh`; no Windows, execute `.\configurar.bat` e depois `.\iniciar.bat`. Entre novamente com seu login e senha; não será pedido código de autenticador.

O pacote público não inclui contas reais, senhas, chaves ou bancos. O arquivo `SHA256SUMS.txt` permite conferir a integridade do ZIP. A primeira configuração precisa de conexão com a internet.
