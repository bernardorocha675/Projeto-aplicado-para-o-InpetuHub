---
name: verificar-tudo
description: Roda toda a verificação local do InPETU Hub (Java, Testar.java, Node da interface e do receptor, node --check, gofmt, go vet e go test do iniciador, segredos e o hook de proteção) e devolve um resumo pronto para o ESTADO-ATUAL.md. Use antes de commit, PR ou ao dizer que algo está pronto.
disable-model-invocation: true
---

# /verificar-tudo

Rode na raiz do projeto:

```bash
.claude/skills/verificar-tudo/scripts/verificar-tudo.sh
```

Com `--rapido`, o `Testar.java` (alguns minutos) fica de fora; use só durante o desenvolvimento, nunca antes de um commit.

O script usa as ferramentas de `aplicativo/.tools/` quando existem (`jdk-21`, `node-24`, `go`, `apache-maven-*`, pasta fora do Git) e senão as do sistema. Se uma ferramenta faltar, a etapa aparece como "não rodado": não declare essa etapa como testada.

## Depois de rodar

1. Leia a linha `RESUMO` e a `RESULTADO`. Se houver falha, mostre a saída da etapa que falhou e não diga que está pronto.
2. Use o resumo, como está, na evidência de testes do `continuidade/ESTADO-ATUAL.md` e na descrição do PR.
3. Diga também o que **não** foi testado: o sistema em que rodou é só um (Linux x64 aqui). Windows, macOS e ARM64 ficam com a matriz da CI; MySQL real e telas no navegador não são cobertos por este script.
