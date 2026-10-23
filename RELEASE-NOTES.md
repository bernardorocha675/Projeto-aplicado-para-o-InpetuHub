# InPETU Hub 0.5.7

Documento e empresa opcionais.

- **Escolha do tipo de documento.** No pedido de reserva e no cadastro de empreendedores, o documento pode ser CNPJ, CPF, documento estrangeiro ou Sem documento. Antes, o CNPJ de 14 dígitos era obrigatório e travava estrangeiros e quem não tem empresa.
- **Conferência pelo tipo.** CNPJ com 14 dígitos, CPF com 11 e documento estrangeiro com 4 a 30 letras ou números; a pontuação é aceita e só o número é gravado. Um documento já cadastrado continua recusado.
- **Sem documento.** O pedido encontra o empreendedor pelo email. Vários cadastros sem documento são permitidos.
- **Empresa/instituição opcional.** Pode ficar em branco no pedido e no cadastro; as telas internas mostram “Não informada” e a busca de reservas não mostra mais “null”.
- **Cadastros antigos preservados.** Empreendedores e pedidos já gravados passam a ser marcados como CNPJ, sem alterar mais nada; pedidos pendentes continuam aprováveis.

Dados, reservas, contas, configurações, chave e backups permanecem na mesma pasta (`InPETU-Hub/0.4`); quem está na 0.5.x ou na 0.4.17 recebe esta versão automaticamente, com backup antes da troca. Migration aditiva `V6__documento_opcional.sql` (o documento aceita vazio e até 30 caracteres, coluna nova do tipo de documento); sem reset. Windows/macOS/Linux x64/ARM64 e Java privado preservados. No macOS, o iniciador continua exigindo macOS 13 ou mais recente.

Publicação condicionada à matriz obrigatória de RELEASES.md.
