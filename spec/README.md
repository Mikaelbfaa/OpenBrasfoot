# Especificações

Estes dois documentos são a **única fonte de verdade permitida** para quem escreve código neste
repositório. Eles descrevem o comportamento observável do Brasfoot 22-23 em forma de fórmulas,
constantes e fluxo. Não contêm, e nunca devem conter, código do jogo original.

| Documento | Cobre |
|---|---|
| [`SIMULATION-SPEC.md`](SIMULATION-SPEC.md) | Motor de partida, modelo de jogador, formações e tática, economia, estrutura de temporada, formato de save |
| [`FORMAT-SPEC.md`](FORMAT-SPEC.md) | Formatos de arquivo `.ban`, `.cfg`, `.ces`, `.bcf` e as tabelas de referência (países, estados, características) |

## Como usar

Cada constante e cada fórmula no código deve citar a seção de onde veio, usando a anotação
`@SpecRef`:

```kotlin
@SpecRef("3.6c")
val shotWeights = doubleArrayOf(5.5, 35.55, 15.0)
```

Isso torna a revisão mecânica: a pergunta é sempre "onde na spec está esse número?".

## Marcações

Os achados são marcados como **CONFIRMADO** (lido diretamente da lógica) ou **INFERIDO** (dedução a
partir de dados ou comportamento). Um achado INFERIDO pode estar errado. Quando a validação
estatística discordar de um número INFERIDO, a spec é que muda, não o teste.

## Lacunas

Comportamento que a spec não cobre vai para `OPEN-QUESTIONS.md` e vira uma issue com o rótulo
`spec-gap`. O motor nunca deve adivinhar em silêncio.

## Regra clean-room

Quem já descompilou, desmontou ou depurou o binário original pode editar `spec/` e `docs/`, mas não
pode editar código. Quem escreve código trabalha somente a partir destes documentos. Reportes de bug
no formato "descompilei e o código real faz X" não são aceitos; o caminho correto é um PR de spec
embasado em comportamento observado.
