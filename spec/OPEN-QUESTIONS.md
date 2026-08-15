# Lacunas e ambiguidades da spec

Comportamento que a spec não determina de forma única. Cada item registra a **resolução adotada** no
código, para que a decisão fique visível em vez de escondida dentro de uma função.

Regra: o motor nunca adivinha em silêncio. Se você encontrar uma lacuna nova, acrescente aqui e abra
uma issue com o rótulo `spec-gap`.

## Seções 3.4 a 3.6

### 1. Divisor do duelo de chute nas temporadas 1 a 4

A seção 3.5 lista `D = 8` para as temporadas 1 a 4 sem dizer a qual família de duelo se aplica, e só
então dá 11 e 10 "a partir da temporada 5".

**Resolução:** 8.0 para os três duelos antes da temporada 5. Guardado num único campo do `RuleSet`,
então uma correção na spec é uma linha.

### 2. Numeração de temporada

Não está dito se a contagem começa em 0 ou 1.

**Resolução:** base 1. A compressão começa quando `temporada >= 5`.

### 3. O piso de 0.2 contra os pesos anti-exploit em 3.6b

Lendo literalmente, o piso roda **depois** da atribuição anti-exploit, então 0.10 e 0.05 viram ambos
0.2 e os dois casos ficam indistinguíveis.

**Resolução:** manter a ordem literal. Existe um teste que afirma que os dois casos são iguais, para
que a suposição fique visível. Evidência a favor: sem o piso, uma defesa com um zagueiro sofreria
**mais** finalizações do que uma com zero, o que não faz sentido.

### 4. O `round` do anti-exploit em 3.6c

A spec escreve `round(wDef x 0.2)` sem dizer a escala.

**Resolução:** `bfRound` para inteiro, como todo `round` da spec. Para um peso de defesa perto de
1.0 isso dá 0, que então bate no piso de 0.2.

### 5. Onde entra o bônus de marcação no meio-campo

A spec diz que a marcação soma 0 / 0.04 / 0.08, mas não diz se antes ou depois do divisor.

**Resolução:** somado ao **total** do meio-campo, antes do divisor fixo. É a única leitura que
produz os 0.008 e 0.016 na escala 0 a 10 que a seção 3.12 cita. No caso degenerado de menos de três
meias o bônus não é aplicado.

### 6. Escala do arredondamento do goleiro fora de posição

`round(GK x 0.2)` pode ser inteiro ou decimal.

**Resolução:** inteiro, na escala 0 a 10. É a única leitura que reproduz o exemplo da seção 5.3: um
jogador de linha com força 70 no gol rende 1.0 contra 7.0 de um goleiro de verdade. Consequência
aceita: força 40 no gol dá legitimamente 0.0, e o piso de 0.2 da resolução de chute cuida disso.

### 7. O que conta como zagueiro

Tanto o anti-exploit de 3.6b quanto o bônus de cabeceio do finalizador falam em "zagueiro" sem dizer
se é posição natural ou slot.

**Resolução:** faixa de slots 3 a 8, coerente com a regra da seção 5.1 de que o motor agrupa por
slot e nunca por posição natural. As duas leituras só divergem para um jogador improvisado.

### 8. Elegibilidade do finalizador

"Sorteio ponderado entre os escalados (exceto goleiro)" não diz se o banco entra.

**Resolução:** somente slots 2 a 25, ou seja, apenas quem está em campo. Reservas têm peso base
zero, mas os bônus por característica são aditivos, então sem o filtro um atacante do banco poderia
finalizar.

### 9. Os números de alavanca da seção 3.16 não fecham

A seção 3.16 afirma que 20 pontos de diferença de força no meio-campo levam o duelo de posse de 55%
para "~69%" e o de chance de 50% para "~56%".

O primeiro não reproduz: a fórmula de 3.6a dá **67,07%** com divisor 8 e 63,84% com divisor 11.
O segundo não reproduz de jeito nenhum, porque **o duelo de chance não lê o meio-campo** - ele
compara ataque contra defesa.

Todos os outros números de 3.16 reproduzem exatamente: 0.614, 0.565, 0.55, 0.50, ~16 chutes,
~12,6 chutes, 9,81%, 8,8%, 11,1%.

**Resolução:** tratar esse parágrafo como narrativo. Testar apenas os valores exatos. Item aberto na
spec.
