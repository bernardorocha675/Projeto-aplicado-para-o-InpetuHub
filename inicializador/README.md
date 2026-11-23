# inicializador/ — iniciador do sistema novo (reescrita 1.x)

Iniciador em Python (decisão 1 do plano), por enquanto só para Linux (decisão 11) e só para computador
(decisão 14). Nome da pasta provisório, para não colidir com o `iniciador/` em Go do sistema antigo.

O que ele faz:
- usa a pasta `InPETU-Hub/1` em `~/.config` (ou em `$XDG_CONFIG_HOME`): `dados/` (banco), `backups/` e `logs/`.
  Nunca toca a `InPETU-Hub/0.4` do sistema antigo;
- deixa abrir só um iniciador por vez (trava de arquivo);
- sobe `runtime/bin/java -jar app/inpetu-hub-sistema.jar` numa porta livre de `127.0.0.1` e abre o navegador
  quando o sistema fica pronto;
- antes de subir, copia o banco para `backups/` (guarda as 10 mais novas) e para um sistema esquecido por uma
  execução anterior;
- para o sistema em ordem pelo arquivo de parada (`sistema.parar`) e só força depois de 20 s;
- grava `logs/iniciador.log` (eventos, sem caminhos nem dados pessoais) e `logs/sistema.log` (o log do sistema).

## Rodar no desenvolvimento

A janela usa o tkinter. No Ubuntu, ele vem no pacote `python3-tk` (`sudo apt install python3-tk`).

```
cd sistema && ./mvnw package -DskipTests && cd ..
cd inicializador
python3 -m inpetu_iniciador --desenvolvimento            # com a janela
python3 -m inpetu_iniciador --desenvolvimento --sem-janela   # sem janela; Ctrl+C para parar
```

`--desenvolvimento` usa o `java` do PATH (JDK 25) e o jar de `sistema/target`. O pacote para o usuário (entrega 11)
traz o Java mínimo em `runtime/` e o jar em `app/`, e o iniciador usa a pasta onde está.

## Testes

```
cd inicializador
python3 -m unittest discover -s testes -t .
INPETU_JAR=../sistema/target/inpetu-hub-sistema.jar python3 -m unittest discover -s testes -t .   # com o jar real
```

Os testes usam um Java falso (`testes/java_falso.py`) e não precisam do tkinter.
