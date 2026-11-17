# inicializador/ — iniciador do sistema novo (reescrita 1.x)

Iniciador em Python (decisão 1 do plano), para Linux e Windows, x64 e ARM64 (decisão 11; sem macOS) e só para computador
(decisão 14). Nome da pasta provisório, para não colidir com o `iniciador/` em Go do sistema antigo.

O que ele faz:
- usa a pasta `InPETU-Hub/1` em `~/.config` (ou em `$XDG_CONFIG_HOME`; no Windows, em `%LOCALAPPDATA%`): `dados/` (banco), `backups/` e `logs/`.
  Nunca toca a `InPETU-Hub/0.4` do sistema antigo;
- deixa abrir só um iniciador por vez (trava de arquivo);
- sobe `runtime/bin/java -jar app/inpetu-hub-sistema.jar` numa porta livre de `127.0.0.1` e abre o navegador
  quando o sistema fica pronto;
- antes de subir, copia o banco para `backups/` (guarda as 10 mais novas) e para um sistema esquecido por uma
  execução anterior;
- para o sistema em ordem pelo arquivo de parada (`sistema.parar`) e só força depois de 20 s;
- grava `logs/iniciador.log` (eventos, sem caminhos nem dados pessoais) e `logs/sistema.log` (o log do sistema).

## Atualização (entrega 10)

Antes de subir o sistema, o iniciador:
1. instala o pacote do próprio executável em `versoes/<versão>` se ele for mais novo que o instalado
   (a primeira abertura sempre instala);
2. consulta as Releases do GitHub pela API pública, sem token, e só aceita tags da linha 1.x
   (`v1.2.3` ou `v1.2.3-alpha.N`, `-beta.N`, `-rc.N`; rascunhos e outras linhas são ignorados);
3. escolhe a mais nova que tenha o pacote deste sistema (`InPETU-Hub-<Linux|Windows>-<x64|ARM64>-v<versão>.zip`) e o `SHA256SUMS.txt` já enviados (uma release incompleta é pulada);
4. baixa, confere o SHA-256 e extrai com segurança: recusa caminhos com `..`, caminhos absolutos e links, e
   limita a quantidade e o tamanho dos arquivos. A extração vai para uma pasta provisória, que só vira
   `versoes/<versão>` depois de conferir o `bundle.json`. A troca da versão atual (`versoes/atual`) é feita de
   uma vez, e ficam guardadas até 3 versões, sempre incluindo a atual;
5. se o iniciador da versão instalada for mais novo que o executável aberto, passa a vez para ele (uma vez só).

Sem internet, com o GitHub fora do ar ou com o limite de consultas atingido, o sistema sobe com a versão
instalada. A janela mostra o motivo, e o log guarda só o tipo (`RATE_LIMITED`, `HTTP_STATUS`, `TIMEOUT`,
`NETWORK`, `INVALID_RESPONSE`, `INTEGRITY`) e o status. Só são aceitos endereços do GitHub, também nos
redirecionamentos.

O pacote (`python3 scripts/build_pacote.py --versao 1.0.0-alpha.N`) tem, numa única pasta dentro do zip:

```
bundle.json                    {"versao": "1.0.0-alpha.1", "plataforma": "linux-x64", "iniciador": "inpetu-hub",
                                "java": "runtime/bin/java", "jar": "app/inpetu-hub-sistema.jar",
                                "arquivos": {"<caminho>": "<SHA-256>", ...}}
inpetu-hub                     o iniciador (PyInstaller, pasta única; no Windows, inpetu-hub.exe sem console)
_internal/                     o Python e o tkinter do iniciador
runtime/                       o Java mínimo (jlink, com os idiomas en, pt e pt-BR; no Windows, bin/java.exe)
app/inpetu-hub-sistema.jar
```

Ao instalar, o iniciador confere o SHA-256 de cada arquivo listado em `arquivos` (um arquivo diferente recusa a
versão). No pacote, a versão do iniciador vem do `bundle.json`. O Java recebe um idioma UTF-8 (sem ele, não abre a
própria pasta quando ela tem acento) e não recebe as bibliotecas do PyInstaller. Teste de fumaça do pacote pronto:
`python3 scripts/smoke_pacote.py dist/<pacote>.zip [--anterior <pacote mais velho>.zip]`.

## Rodar no desenvolvimento

A janela usa o tkinter. No Ubuntu, ele vem no pacote `python3-tk` (`sudo apt install python3-tk`).

```
cd sistema && ./mvnw package -DskipTests && cd ..
cd inicializador
python3 -m inpetu_iniciador --desenvolvimento            # com a janela
python3 -m inpetu_iniciador --desenvolvimento --sem-janela   # sem janela; Ctrl+C para parar
python3 -m inpetu_iniciador --pacote "PASTA DO PACOTE"        # como o executável: instala, atualiza e sobe
```

`--desenvolvimento` usa o `java` do PATH (JDK 25) e o jar de `sistema/target`. O pacote para o usuário
traz o Java mínimo em `runtime/` e o jar em `app/`, e o iniciador usa a pasta onde está.

## Testes

```
cd inicializador
python3 -m unittest discover -s testes -t .
INPETU_JAR=../sistema/target/inpetu-hub-sistema.jar python3 -m unittest discover -s testes -t .   # com o jar real
```

Os testes usam um Java falso (`testes/java_falso.py`) e um GitHub falso (`testes/github_falso.py`), e não
precisam do tkinter nem de internet.
