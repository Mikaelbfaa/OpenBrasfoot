# OpenBrasfoot

Uma versão Open Source e gratuita do Brasfoot, já que a versão original não recebe mais
atualizações. Os assets e outros arquivos de autoria da equipe do Brasfoot ainda precisam ser
baixados pelo site oficial. O jogo foi/está sendo escrito do zero numa plataforma moderna.

## Situação atual

**Muito cedo.** O projeto está na v0.1, que é a fase do motor: nenhuma interface gráfica ainda.
A prioridade é acertar a matemática da simulação e provar que ela bate com o comportamento
observado do jogo original, antes de construir telas em cima.

Já funciona:

- Gerador aleatório determinístico, com derivação de semente por posição no mundo
- Grade de 25 slots, posições, características e tipos de competição
- `effectiveStrength`, a função de força efetiva que é o átomo do motor de partida
- Agregados de linha, com os divisores fixos do original e a ordem de lista que decide quem entra
- Os três duelos: posse, criação de chance e resolução de chute, com o sorteio do finalizador
- `RuleSet`, com os conjuntos `CLASSIC` e `MODERN`, onde os defeitos do original viram dado e não `if`
- Testes de arquitetura que impedem I/O, relógio, aleatoriedade de plataforma e não determinismo
- Verificador de estilo de comentário

Ainda falta, para uma partida completa: o laço de tiques, tipos de gol, disciplina e lesões,
energia, assistências e notas.

## Como compilar

Precisa apenas de um JDK 21. O Gradle vem pelo wrapper.

```
./gradlew build
```

Rodar todas as verificações, incluindo testes de arquitetura e vetores dourados:

```
./gradlew check
```

## Filosofia

O jogo é uma **reimplementação comportamental**, no espírito do OpenTTD. Não contém código, dados
nem arte do Brasfoot. Tudo foi escrito a partir de duas especificações em [`spec/`](spec/), que
descrevem fórmulas, constantes e fluxo observáveis.

Duas consequências práticas:

1. **Determinismo.** O original sorteia sem semente e não é reproduzível. Aqui uma carreira inteira
   se reproduz a partir da semente, e um relatório de bug é a semente mais o log de comandos.
2. **Dois conjuntos de regras.** O `CLASSIC` reproduz o original fielmente, inclusive os cerca de
   vinte defeitos documentados. O `MODERN` corrige o que estava claramente quebrado. O `CLASSIC`
   existe também por um motivo técnico: é a única forma de provar que o motor está certo,
   comparando a saída estatística com a do jogo original.

## Dados e arte

O repositório **não distribui** times, jogadores, escudos ou uniformes. Um importador lê a sua
própria instalação do Brasfoot na sua máquina, e um conjunto de dados aberto, feito pela
comunidade, permitirá jogar sem precisar do original.

## Ajuda procurada: saves `.s22`

O formato de save do original usa Kryo com campos gravados por posição, sem nomes. Documentar isso
exige exemplos reais. **Se você tem saves antigos do Brasfoot 22-23, guarde-os.** Eles serão úteis
quando essa etapa chegar. Não envie nada ainda, e não envie saves com informação pessoal.

## Roadmap resumido

| Versão | Tema |
|---|---|
| v0.1 | Motor de partida headless, validado estatisticamente |
| v0.2 | Importador dos arquivos originais e geração de mundo |
| v0.3 | Temporada completa, tabelas, mata-mata, evolução de jogadores |
| v0.4 | Primeira interface, instalador nativo |
| v0.5 | Escalação, tática, gestão de elenco |
| v0.6 | Economia, mercado, diretoria |
| v0.7 | Partida ao vivo |
| v0.9 | Regras `MODERN` e conjunto de dados aberto |

Isso é trabalho de anos em ritmo de projeto voluntário. Prefira acompanhar por milestone.

## Contribuindo

Leia [`CONTRIBUTING.md`](CONTRIBUTING.md) antes de abrir um PR. Existe uma regra clean-room que não
é negociável: quem descompilou o jogo original não escreve código aqui.

## Licença

Código sob [GPL-3.0-or-later](LICENSE). As especificações em `spec/` sob CC BY-SA 4.0.
Veja [`NOTICE.md`](NOTICE.md).
