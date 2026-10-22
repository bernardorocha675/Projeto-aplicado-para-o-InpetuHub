# InPETU Hub 0.5.6

Telefone com país.

- **Escolha do país no telefone.** No pedido de reserva e no cadastro de empreendedores, o telefone tem um seletor de país (Brasil já marcado). No Brasil continua a máscara com DDD, como (48) 99808-2043; nos outros países, basta digitar o número local.
- **País preenchido sozinho.** Digitar + e o código (por exemplo, +351) escolhe o país no seletor. Escolher uma cidade nas sugestões (por exemplo, “Lisboa, Distrito de Lisboa, Portugal”) já marca o país do telefone, se o telefone ainda estiver vazio.
- **Número conferido pelo país.** O sistema confere se o número existe no formato daquele país e o grava no padrão dele (Brasil como (48) 99808-2043; outros países como +351 912 345 678). A conferência usa a biblioteca libphonenumber, do Google.
- **Nada muda nos cadastros antigos.** Telefones gravados antes continuam aceitos; pedidos pendentes podem ser aprovados normalmente, e ao editar um cadastro o número antigo só é conferido de novo se for alterado.

Dados, reservas, contas, configurações, chave e backups permanecem na mesma pasta (`InPETU-Hub/0.4`); quem está na 0.5.x ou na 0.4.17 recebe esta versão automaticamente. Não há migration nem reset. Dependência nova: libphonenumber 9.0.41 (Apache 2.0), sem alertas na consulta OSV. Windows/macOS/Linux x64/ARM64 e Java privado preservados. No macOS, o iniciador continua exigindo macOS 13 ou mais recente.

Publicação condicionada à matriz obrigatória de RELEASES.md.
