# Validação — Android 0.1.0-experimental.1

Entrega experimental independente, em 17/10/2026. Código/tag: `6c0606e59a9afc78084a1592591d8b163605a4cd`, `android-v0.1.0-experimental.1`.

## Evidências

- [Validação prévia em branch — run 39059231093](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39059231093): build, assinatura, instalação e 16 cenários aprovados.
- [Validação e publicação em main — run 39059503298](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39059503298): os mesmos 16 cenários passaram novamente; job de publicação aprovado.
- [Prerelease Android](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/android-v0.1.0-experimental.1): APK, fontes, relatório, SHA256SUMS.txt e backup cifrado da assinatura. Cinco assets conferidos; tag corresponde ao código testado. Sem substituição de releases/assets anteriores; latest estável continua v0.4.5.

Build com JDK 17, SDK API 35, Build Tools 35.0.0, aapt2/javac/D8/zipalign/apksigner. Assinatura APK v2/v3 e alinhamento verificados. Emulador Android 15 x86_64/Pixel 4, Wi-Fi/dados desligados. Manifest sem permissões e sem backup automático do Android; verificação de fontes/segredos aprovada. Capturas reais do login, agenda, rejeição de capacidade zero e cadastro corrigido foram inspecionadas; conteúdo de teste sintético.

## Cenários aprovados

| Área | Verificação |
| --- | --- |
| Acesso | Cinco contas, hash PBKDF2, recusa de senha errada, normalização de login, permissões e logout. |
| Espaços | Zero/negativos/frações/estouro recusados; correção aceita; edição inválida preserva os dados; agenda inicial vazia. |
| Reserva | Contatos, capacidade, expediente, datas passadas/365 dias, pendentes sem ocupação, aprovação com vínculo, conflitos e intervalos adjacentes. |
| Valores | Taxas explicitamente configuradas, preços 2026, percentuais, sinal de domingo/feriado e arredondamento. |
| Cotas | Uso apenas na aprovação, saldo insuficiente, cancelamento/penalidade e força maior. |
| Situações | Expiração, justificativa de recusa, pagamentos e conclusão após o fim. |
| Dados | Backup sem contas/logs, restauração em banco vazio, rollback de backup inválido e recusa de sobrescrita. |
| Persistência | Dados mantidos na reabertura e nova sessão exige login. |
| Logs | Versão e tipo de erro presentes; campos livres, contatos e senhas ausentes. |
| Interface | Login nativo, cadastro de espaço com capacidade zero, mensagem, correção para 25 e persistência na listagem. |

São 15 cenários de regras/banco e um cenário de interface, reunindo múltiplas verificações. O relatório publicado contém `TESTES ANDROID APROVADOS: 16`. A instrumentação usa bancos descartáveis; nenhum banco do usuário foi utilizado, resetado ou incluído no APK.

## Integridade e recuperação

SHA-256 do APK publicado: `5d5609f5426b71cb3972a954e70d3a734736f41851093af7fad5c03d31729a86`.

Certificado de assinatura do APK, SHA-256: `1594eb9400b91c7d1849e31408181776473ab7bab1868246f88a3286066f3a69`.

APK, relatório e backup cifrado obtidos do artifact do run de publicação coincidem com os digests dos assets da release. O backup cifrado foi aberto com a chave privada de recuperação fora do Git; o PKCS#12 recuperado foi lido e seu certificado coincide com a assinatura validada no APK. Senha/chave não foram exibidas nem versionadas. Procedimento de manutenção em HANDOVER-ANDROID.md. O ZIP privado de recuperação deve ser guardado pelo responsável; o .p7m público sozinho não permite abrir a chave.

## Limites e aceite pendente

Não houve teste em celular físico, ARM, Android 8.0 mínimo ou todas as versões posteriores. Validar instalação/atualização, fontes grandes, acessibilidade, rotação e uso prolongado em aparelhos da equipe. Campos não salvos não sobrevivem à recriação da atividade. Desinstalar/limpar dados apaga o banco; exportar backup antes da manutenção.

Android não sincroniza com H2 ou outros celulares, não contém CRM/SMTP/edição de reservas/atualizador/envio ao GitHub e tem a diferença de recálculo histórico de cotas documentada no handover. Testes de regras não comprovam equivalência integral. A matriz desktop adicional é o [run 39059503236](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39059503236); seu resultado não comprova funcionamento Android. Render e envio de computadores continuam pendentes.
