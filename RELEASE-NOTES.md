# InPETU Hub 0.3.2

- Login básico com usuário e senha, sem segundo fator.
- Usuários aceitam maiúsculas, acentos e espaços de digitação.
- Novos acessos do pacote privado são carregados ao iniciar, mesmo em um banco já existente.
- Erros de senha não bloqueiam temporariamente o modo local.
- Senhas de acesso aceitam 8–64 caracteres. A senha continua sensível a maiúsculas e minúsculas.

## Como usar os novos acessos

Atualize o código da pasta **versão 0.3** e aplique o pacote privado de acessos recebido separadamente. Siga `ACESSOS.md`: o instalador privado copia as contas para o lugar correto e guarda o arquivo anterior. Depois inicie o servidor e entre com os dados novos. O ZIP público não inclui as cinco senhas.

Linux/macOS: `bash configurar.sh` e depois `bash iniciar.sh`. Windows: `.\configurar.bat` e depois `.\iniciar.bat`. Quem já configurou esta cópia pode executar somente o comando de iniciar.

## Instalações existentes

Faça backup com o servidor parado e preserve `data/`, `.env.local` e `.secrets/`. Não é necessário apagar o banco. A migration V4 acrescenta o controle de importação das contas; V1–V3 permanecem intactas. Uma entrada nova ou alterada no arquivo privado é aplicada uma vez. Repetir o mesmo pacote não desfaz uma senha alterada pelo painel; contas fora do pacote não são apagadas em bancos já provisionados.

Reservas, cadastros, perfis, logs e backups continuam disponíveis. A limitação de tentativas de login permanece no perfil de produção; o modo local foi simplificado para os testes da equipe.
