# InPETU Hub

Sistema de agendamento de espaços e gestão de reservas, com agenda pública e área interna para a equipe do Hub.

Projeto acadêmico desenvolvido na disciplina de **Projeto Aplicado do UniSENAI**, em parceria com o **InPETU Hub**.

## Equipe

- Bernardo Rocha
- João Mussiol
- João Feitosa
- Guilherme Batista

## Versão atual

[**Versão 0.4.5**](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.5) — amplia os logs de erros e melhora a recuperação e o acompanhamento dos relatórios.

## Android experimental

[Android 0.1.0-experimental.1](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/android-v0.1.0-experimental.1) — APK independente, com banco no celular e uso offline. Recursos e limitações no [guia Android](docs/ANDROID.md).

## Usar o aplicativo

Baixe o pacote completo do seu sistema em [Releases](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/latest), extraia e abra o iniciador. O pacote inclui o aplicativo e o Java necessário.

- [Instalação e execução](docs/COMO-INICIAR.md)
- [Acessos de demonstração](docs/ACESSOS.md)
- [Atualizações e dados](docs/ATUALIZACOES.md)
- [Logs e relatórios](docs/RELATORIOS.md)

## Organização do repositório

| Pasta | Conteúdo |
| --- | --- |
| `android/` | Aplicativo Android experimental independente e testes no emulador. |
| `aplicativo/` | Aplicativo Java e testes de regras, segurança e integração. |
| `iniciador/` | Código Go do executável de instalação, atualização e início. |
| `receptor/` | Serviço auxiliar de recebimento dos relatórios. |
| `docs/` | Guias, testes e handover da entrega atual. |
| `Testes/` | Relatórios publicados e configuração de envio. |
| `continuidade/` | Contexto e decisões para continuidade com o Claude Code. |

As versões anteriores permanecem nas [releases](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases) e no histórico Git. O código atual reúne os componentes necessários ao executável e ao seu ciclo de testes/publicação.

- [Handover técnico](docs/HANDOVER.md) · [Estrutura e desenvolvimento](docs/ESTRUTURA.md)
- [Guia de testes](docs/TESTAR.md) · [Publicação de versões](RELEASES.md)
- [Continuidade e migração para o Claude Code](continuidade/README.md)

---

<p align="right"><sub>Desenvolvido com apoio de ferramentas de inteligência artificial.</sub></p>
