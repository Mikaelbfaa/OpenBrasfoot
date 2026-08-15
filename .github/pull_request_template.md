# O que muda

<!-- Uma frase. O porquê importa mais que o quê. -->

## Seção da spec

<!--
Se isto toca o motor, diga de qual seção de spec/ a regra veio, por exemplo 3.6c.
Se o comportamento não está na spec, pare: abra uma issue com o rótulo spec-gap.
-->

## Checklist

- [ ] `./gradlew check` passa
- [ ] Constantes novas carregam `@SpecRef` apontando para a seção de onde vieram
- [ ] Nenhum `if (ruleSet.id == ...)` foi adicionado ao `:engine`
- [ ] Comentários são docstrings, só ASCII, sem markdown dentro
- [ ] Nenhum arquivo de dados do jogo original foi commitado
- [ ] Valores esperados nos testes foram calculados a partir da spec, não copiados da saída do código

## Declaração clean-room

Obrigatória para qualquer mudança em código.

- [ ] Declaro que escrevi esta contribuição **somente** a partir de `spec/` e do meu próprio
      conhecimento, e que **não consultei** código descompilado, desmontado ou saída de depurador
      do jogo original.

<!--
Se você já descompilou o jogo, você é do time de spec e pode editar spec/ e docs/, mas não código.
Isso não é punição, é o que dá ao projeto uma posição defensável. Ver CONTRIBUTING.md.
-->
