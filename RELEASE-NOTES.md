# InPETU Hub 0.4.14

Passa a abrir no macOS 12 Monterey.

- No macOS, o iniciador da 0.4.13 exigia macOS 13 Ventura ou mais recente, por causa da ferramenta usada para gerá-lo, e não abria em Macs com Monterey (inclusive Intel de 2016). Os pacotes de macOS passam a ser gerados para abrir no macOS 12 ou mais recente, e o app declara esse mínimo ao sistema.
- A CI passa a recusar um pacote de macOS cujo iniciador ou Java privado exija versão acima do macOS 12, para o problema não voltar.
- Windows e Linux não mudam de comportamento.

Dados, reservas, contas, configurações, chave e backups permanecem no mesmo local da família 0.4.x. Não há migration, reset, mudança de dependências ou ativação externa. Windows/macOS/Linux x64/ARM64 e Java privado preservados; Android continua retirado.

Publicação condicionada à matriz obrigatória de RELEASES.md.
