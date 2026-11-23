# InPETU Hub CRM

Sistema de gestão de relacionamento (CRM) e de reservas de espaços feito para o InPETU Hub, como projeto aplicado do UniSENAI. Ele roda no seu computador e abre no navegador.

## Sobre o projeto

A equipe do Hub guardava as informações dos empreendedores em várias planilhas, sem histórico dos atendimentos e com dificuldade para organizar as reservas. O sistema está sendo construído para reunir tudo em um só lugar. Já estão prontos:

- **Agenda pública:** qualquer pessoa consulta os dias e envia um pedido de reserva.
- **Solicitações e reservas:** a equipe aprova, recusa ou cancela.
- **Espaços e painel:** cadastro dos espaços e visão das próximas reservas.

## Baixar

### Versão 1.0.0-alpha.1

Esta é uma versão de teste. **Use com dados fictícios.** Escolha o pacote do seu computador; não é preciso instalar mais nada.

| Computador | Executável |
| --- | --- |
| Windows (Intel/AMD) | [Baixar a 1.0.0-alpha.1](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v1.0.0-alpha.1/InPETU-Hub-Windows-x64-v1.0.0-alpha.1.zip) |
| Linux (Intel/AMD) | [Baixar a 1.0.0-alpha.1](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v1.0.0-alpha.1/InPETU-Hub-Linux-x64-v1.0.0-alpha.1.zip) |

**Ainda não tem:** empreendedores, atendimentos, visitas, colaboradores e as telas de administração e configuração. Por enquanto, só as reservas da persona P1 podem ser aprovadas.

## Como executar

### 1. Abrir

1. Baixe o pacote do seu computador.
2. **Extraia o ZIP inteiro** e abra a pasta extraída. Não abra o programa de dentro do ZIP.
3. Abra o `inpetu-hub.exe` no Windows ou o `inpetu-hub` no Linux.
4. Espere a janela do iniciador mostrar **Rodando**. O navegador abre sozinho na agenda. Se não abrir, clique em **Abrir navegador**.

### 2. Entrar

A agenda e o pedido de reserva são públicos. Para a parte da equipe, clique em **Área Interna** e use um dos [acessos de demonstração](docs/GUIA.md#acessos-de-demonstração).

### 3. Encerrar

Feche a janela do iniciador e espere ela sumir: o sistema salva o banco antes de sair. Fechar só a aba do navegador não encerra o sistema.

### Se algo não abrir

Confira se o arquivo veio deste repositório e veja o seu caso:

- **Aviso do Windows:** o programa não é assinado. Escolha *Mais informações → Executar assim mesmo*.
- **Linux não executa:** nas propriedades do arquivo, permita a execução. Pelo terminal, dentro da pasta: `chmod +x inpetu-hub runtime/bin/java && ./inpetu-hub`.
- **“O iniciador já está aberto”:** use a janela que já está aberta.
- **“Não iniciou”:** leia a mensagem na janela e clique em **Iniciar** para tentar de novo.

### Dados e atualizações

Os dados ficam só no seu computador: em `%LOCALAPPDATA%\InPETU-Hub\1` no Windows e em `~/.config/InPETU-Hub/1` no Linux. A partir da segunda abertura, o sistema faz uma cópia do banco antes de iniciar.

Ao abrir, o iniciador procura versões novas e atualiza sozinho. Sem internet, usa a versão já instalada.

## Equipe e créditos

Projeto aplicado oficial do **InPETU Hub**, desenvolvido na disciplina de **Projeto Aplicado do UniSENAI** por:

- Bernardo Rocha
- João Mussiol
- João Feitosa
- Guilherme Batista

## Desenvolvimento

| Assunto | Onde |
| --- | --- |
| Código | `sistema/`, `inicializador/` e `scripts/` |
| Como rodar e testar | [Sistema](sistema/README.md) e [iniciador](inicializador/README.md) |
| Andamento | [Plano da reescrita](docs/PLANO-REESCRITA.md) |

Veja também o [guia de uso](docs/GUIA.md), [como contribuir](CONTRIBUTING.md), a [política de segurança](.github/SECURITY.md) e [toda a documentação](docs/README.md).

---

<p align="right"><sub>Desenvolvido com apoio de ferramentas de inteligência artificial.</sub></p>
