# Handover — Android 0.1.0-experimental.1

## Decisão e escopo

Base desktop: main c4f26fba976bcb7f917d7096fb2f36298abae155, release estável 0.4.5. Em 17/10/2026, o usuário pediu Android experimental e selecionou **Funcionar sozinho**. O port nativo fica em android/, com versão, ciclo de publicação e banco próprios. Não substituir nem alterar assets desktop.

Classes: MainActivity (telas nativas e seleção de arquivos), Store (SQLite, transações, contas, operações), Rules (validação, horários, taxação, dinheiro/sinal). Sem servidor, WebView, JNI, dependências Maven ou permissão INTERNET. Usa SDK oficial API 35, Build Tools 35.0.0, Java 8 bytecode/D8, Java 17 de build, mínimo Android API 26. Um APK sem bibliotecas nativas atende arquiteturas compatíveis com o Android, mas testes de uma arquitetura não comprovam todas.

Store cria contas somente no banco novo, com PBKDF2-HMAC-SHA256/60.000 iterações/sal de 16 bytes/hash de 32 bytes. Bernardo é ADMIN. Senhas públicas de demonstração são as mesmas da linha desktop; não há configuração de credencial de produção nessa entrega. Sessão autenticada em memória, sem autologin após recriar processo/atividade.

SQLite schema 1, foreign keys e transações nas operações compostas. Não há migration destrutiva: onUpgrade sem implementação falha preservando o banco. Backups são JSON Android, sem contas/logs, restaurados apenas em instalação vazia e em transação. Contatos no backup são privados; logs exportados contêm apenas campos controlados. Eventos limitados aos últimos 5.000; não presumir que onStop significa encerramento definitivo.

## Regras portadas e diferenças

A base para comparação é DisponibilidadeService, TaxacaoService, PrecoService, ReservaService, CotaService e migration V2 do desktop 0.4.5. Apenas aprovadas conflitam/usam cota; pendentes duram 24h. Limite de 365 dias, expediente configurado, capacidade, intervalos adjacentes, antecedência de 15 dias para cota, personas P1–P6, percentuais 100/75/50, sinal 30% ou 60% em domingo/feriado, taxas explícitas, pagamento/conclusão e cancelamento com estorno/penalidade.

Não há reutilização binária do Spring/JPA no Android. Regras estão adaptadas, com testes específicos. A cota é recalculada por persona com dívida mensal; alterar seu limite muda também os meses anteriores, enquanto o desktop preserva limites de cotas existentes. CRM, edição de reserva, SMTP, links públicos, MFA, importação H2, sincronização, envio de logs e atualizador automático não estão incluídos. Essas diferenças aparecem no guia e na própria área Sobre. Não ampliar a declaração de compatibilidade para paridade integral.

## Build, testes e publicação

Com JDK 17 e SDK oficial, executar assinatura/configuração de ambiente e python android/scripts/build.py. O build usa aapt2/javac/D8/zipalign/apksigner e recusa divergência entre VERSION, manifest e Store. Workflow .github/workflows/android-experimental.yml instala os pacotes oficiais fixados, verifica ausência de permissões e roda instrumentação real no emulador Android 15 x86_64, com Wi-Fi/dados desativados.

TestRunner usa bancos com prefixo test-, nunca o banco de um usuário físico. São 15 cenários de regras/persistência e um fluxo de interface: cinco logins, permissões, capacidade/reenvio, agenda vazia, datas/contato, conflitos/adjacência, taxas, cotas/cancelamento, força maior/saldo, expiração, pagamento/conclusão, logs, backup/rollback, reabertura e taxação/sinal. O fluxo de UI preenche login, cadastra espaço com erro/correção e captura telas sintéticas. O resultado precisa conter TESTES ANDROID APROVADOS: 16. Compilação e instalação não equivalem a teste em aparelho físico; consultar a execução da tag antes de declarar publicado.

Publicação só em push de main após sucesso do job Android. Tag android-v0.1.0-experimental.1, prerelease e latest=false. A versão de computadores e seu atualizador continuam 0.4.5. Assets: APK, fontes, relatório, checksums e, na primeira publicação, backup criptografado da assinatura. Nunca publicar APK de testes ou keystore aberto. Publicador não substitui release existente.

## Assinatura e recuperação

A chave privada de recuperação do responsável foi gerada fora dos fontes e entregue separadamente em ZIP privado. Só o certificado público signing-recovery.crt acompanha o repositório. A primeira CI gera uma assinatura privada Android e criptografa keystore/senha com CMS/AES-256 para esse certificado. O arquivo assinatura-android-backup.p7m acompanha a release; o segredo de recuperação não está nele nem no repositório.

Antes de uma nova publicação, recuperar o keystore com o ZIP privado e o .p7m e configurar os secrets ANDROID_KEYSTORE_B64 e ANDROID_KEYSTORE_PASSWORD no GitHub. signing.py recusa gerar outra chave se já existe uma release Android. Não contornar essa falha: identidade nova pode impedir atualização do APK instalado. Não desinstalar nem apagar dados para resolver uma assinatura incompatível.

Recuperação de manutenção (arquivos privados fora do Git): openssl cms -decrypt -binary -inform DER -in assinatura-android-backup.p7m -recip signing-recovery.crt -inkey recuperacao-android.pem -out signing.zip. O ZIP contém signing.p12 e password.txt. O certificado do APK é independente do certificado usado para criptografar o backup. A perda do ZIP privado e do keystore compromete futuras atualizações.

## Pendências e limites

Validar em aparelhos físicos Android e versões mínimas/arquiteturas, rotação/fontes grandes/acessibilidade e ciclo de vida com uso prolongado. Os campos ainda não salvos não sobrevivem à recriação da atividade. Processo em background não é servidor nem serviço permanente. Backup Android é independente do H2. Não houve publicação na Play Store, assinatura comercial, sincronização ou ativação Render. A configuração de relatórios dos PCs continua pendente e não é parte deste pedido.
