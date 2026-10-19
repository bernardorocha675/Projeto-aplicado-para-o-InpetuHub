# InPETU Hub 0.4.12

Downloads em conexões lentas, avisos visíveis e proteção do endereço local.

- O download de atualização não tem mais limite total de 5 minutos: continua enquanto os dados chegam e mostra o progresso em MB. Só é interrompido após 60 segundos sem receber nada; então a versão instalada continua em uso e o download não é repetido ao clicar Iniciar.
- Quando o iniciador não consegue abrir, uma janela de aviso explica o motivo (Windows, macOS e Linux com zenity/kdialog/xmessage). Antes, nada acontecia.
- O sistema local recusa endereços diferentes de 127.0.0.1/localhost, impedindo que uma página externa use o aplicativo aberto no computador.
- Restos de downloads interrompidos são removidos automaticamente; pastas de versão danificadas continuam preservadas.
- Avisos de envio sem internet deixam de se repetir a cada minuto nos registros. A consulta de atualizações é reaproveitada por 10 minutos, poupando o limite do GitHub.
- Gravação de arquivos no Windows tenta de novo quando o antivírus segura o arquivo por instantes.
- Documentação reorganizada: guias sem números de versão antigos, índice atualizado e histórico da continuidade arquivado.

Dados, reservas, contas, configurações, chave e backups permanecem no mesmo local da família 0.4.x. Não há migration, reset, mudança de dependências ou ativação externa. Windows/macOS/Linux x64/ARM64 e Java privado preservados; Android continua retirado.

Publicação condicionada à matriz obrigatória de RELEASES.md.
