# Como contribuir

Obrigado pelo interesse. Antes de qualquer coisa, leia a regra clean-room. Ela é o que mantém o
projeto viável.

## A regra clean-room

O projeto tem **dois papéis**, e ninguém ocupa os dois ao mesmo tempo.

| Papel | Pode editar | Não pode |
|---|---|---|
| **Time de spec.** Qualquer pessoa que já descompilou, desmontou ou depurou o jogo original | `spec/`, `docs/`, discussão em issues | qualquer código |
| **Time de implementação.** Todo o resto | todo o código | descompilar o original, ou ler saída de descompilador em qualquer lugar |

Se você já abriu o jogo original num descompilador, você é do time de spec. Isso não é uma punição,
é o que dá ao projeto uma posição defensável: o código é escrito a partir de uma descrição de
comportamento, não a partir da obra de outra pessoa.

Consequências práticas:

- **Não aceitamos relatos do tipo "descompilei e o código real faz X".** O caminho correto é um PR
  em `spec/` embasado em **comportamento observado** jogando o jogo.
- Todo PR de código exige a declaração do template: escrito somente a partir de `spec/` e do próprio
  conhecimento, sem consultar descompilação.
- A CI recusa PRs que contenham identificadores típicos de descompilação fora de `spec/`.

## Fluxo

1. Abra uma issue antes de um PR grande.
2. Faça um fork, crie um branch com nome descritivo.
3. Commits atômicos: uma mudança lógica por commit, e o build passa em cada um.
4. Mensagem no padrão Conventional Commits, por exemplo `feat(engine):`, `fix(match):`,
   `test(model):`, `docs(spec):`.
5. `./gradlew check` verde antes de abrir o PR.

## Regras de código

### Toda constante cita a spec

```kotlin
@SpecRef("3.6c")
val shotWeights = doubleArrayOf(5.5, 35.55, 15.0)
```

A revisão então é mecânica: onde na spec está esse número. Um número sem `@SpecRef` não entra.

### Nunca ramifique pelo conjunto de regras

Uma divergência entre `CLASSIC` e `MODERN` vira **um campo do `RuleSet`**, não um `if`. Um PR que
adiciona `if (ruleSet.id == ...)` no `:engine` é recusado, e o teste de arquitetura pega isso
sozinho.

A ordem de preferência é: constante, depois tabela, depois objeto de estratégia, e só em último caso
um booleano.

### O motor é puro

O módulo `:engine` não faz I/O, não lê relógio, não registra log, não conhece interface gráfica e
não usa aleatoriedade de plataforma. Também não itera sobre `HashMap` nem usa funções
transcendentais. Cada uma dessas coisas produz carreiras que não se reproduzem a partir da semente,
o que quebra o fluxo de relato de bug para todo mundo. Há um teste de arquitetura para cada uma.

### Estilo de comentário

- Apenas docstrings. Nada de comentário narrando a linha seguinte.
- Somente ASCII. Sem travessão, aspas curvas, setas ou símbolos decorativos.
- Sem sintaxe markdown dentro de comentário. Markdown é para arquivos `.md`.

Um bom docstring diz o que o código não consegue dizer sozinho, normalmente de qual seção da spec a
fórmula veio e qual peculiaridade está sendo reproduzida de propósito.

```kotlin
/**
 * Força efetiva numa escala de zero a dez. Ver SIMULATION-SPEC seção 3.3.
 * Recalculada a cada uso no original, sem cache. Energia não entra de propósito.
 */
```

O `./gradlew check` verifica isso automaticamente.

## Reportando bugs

Quando o motor estiver reproduzindo carreiras, todo relato de bug precisará da **semente** e do
arquivo de replay. Sem isso não dá para investigar, e a issue fica marcada como `needs-replay`.
O determinismo existe justamente para tornar essa exigência justa.

## Lacunas na spec

Se o comportamento que você precisa implementar não está descrito, **não adivinhe em silêncio**.
Abra uma issue com o rótulo `spec-gap` e registre em `spec/OPEN-QUESTIONS.md`.

## Dados e arte

Nunca faça commit de arquivos do jogo original. As extensões estão bloqueadas no `.gitignore` e na
CI. Um commit acidental desses significa reescrever histórico já publicado.
