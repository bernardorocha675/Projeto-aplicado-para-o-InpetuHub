# Validação — 0.4.5

Fix de logs publicado na [v0.4.5](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.5), código testado e tag `1fac5b0bf5f432db48f85f63181c037859817f23`. O [run 39028486594](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39028486594) concluiu os 11 jobs com sucesso. Conferidos os 12 arquivos carregados; as nove releases anteriores preservaram IDs, nomes, arquivos/tamanhos/digests.

Validação local aprovada: 99 testes Java (97 no roteiro completo, mais dois de SMTP simulados), 30 testes Go e go vet, cinco testes Node da agenda, dois da interface administrativa e sete do receptor/coletor. Passaram o roteiro completo HTTP/perfis/contas/backup, empacotamento e smoke do executável Linux real.

Regressões novas cobrem erro inesperado com página simples/requestId/tipo/pontos no log sem mensagem livre; regras e integridade retornando 400/409; data inválida registrada sem mostrar erro de conversão; parâmetro ausente 400, recurso ausente 404 e autorização 403. SMTP é simulado, sem envio real. Go cobre gravação final incompleta sem upload prematuro, recuperação na abertura, reparo de sessão legada preservando conteúdo e diagnóstico de envio sem segredo. O smoke cria um erro controlado na reserva do executável real e confere sua presença no relatório sanitizado.

A matriz foi aprovada nos seis ambientes: Ubuntu 24.04 x64/ARM64, macOS 15 Intel/Apple Silicon, Windows Server 2025 x64 e Windows 11 ARM64, com os gates de fonte/segredos/dependências/CodeQL/relatório/publicação também aprovados. Mantém JDK 21 no pacote, alvo Java 17 e Go 1.27.2.

O transporte e recibos são testados com servidor simulado. Nenhum workspace Render foi selecionado, serviço criado ou endpoint ativado. O envio real dos PCs não foi comprovado e continua pendente da confirmação do workspace.

Os testes não comprovam todos os navegadores/edições, queda física de energia/disco ou cada falha interna do navegador. Não há implantação MySQL/SMTP/TLS de produção. Mantém-se a limitação conhecida de interpretação de consulta CSRF no CodeQL. Outros itens do PDF permanecem pendentes.

O smoke real com erro controlado, seus eventos/versões e a ausência do conteúdo do formulário passou nos seis ambientes. Este fechamento acompanha main; os documentos no ZIP de código são o instantâneo anterior à CI. A release imutável não foi substituída.
