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

## Seção 4 - criação do mundo

### 10. A spec não define ordem de sorteio na criação do mundo

A seção 0 diz que o original cria um gerador novo, sem semente, a cada sorteio. Não existe ordem a
imitar, e nenhuma seção descreve a criação do mundo como uma sequência.

**Resolução:** definimos a nossa. A semente deriva por posição no mundo, nunca por contagem de
sorteios: `raiz -> WORLDGEN -> clube -> jogador`, com a chave do clube vinda do `fileRef`. Dentro de
um jogador os sorteios são sequenciais, na ordem estilo, força, atributos, bônus de característica,
contrato. Isso torna irrelevante a ordem em que os clubes são gerados, que é o que permite gerar em
paralelo sem mudar resultado.

### 11. Três linhas da tabela 4.2 não dão fórmula para Gol

Lateral ofensivo, volante e meia armador listam seis atributos. Falta **Gol** nos três. As outras
quatro linhas dão os sete, e toda linha de jogador de linha usa o mesmo idioma: `Gol = 1+rnd(k)`,
com k igual a 4 no lateral defensivo, 7 no zagueiro e 6 no atacante.

**Resolução:** `Gol = 1+rnd(4)` nas três linhas omissas.

Vale registrar por quê, porque a leitura oposta parece mais conservadora e não é. Deixar o atributo
em zero também é uma escolha que a spec não faz: zero vem do vetor zerado da implementação, não do
documento. As duas leituras inventam alguma coisa.

O que decide é a seção 3.3. Ela já define o mecanismo para punir quem joga fora da posição: a
divisão pela metade. Deixar `Gol` em zero aplica uma segunda punição, não projetada, e só em três
dos sete arquétipos. Além disso cria uma assimetria arbitrária entre o lateral defensivo, que
recebe `1+rnd(4)`, e o lateral ofensivo, que é a mesma posição e difere apenas no estilo.

O efeito é pequeno: com os pesos do gol (0,60 para Gol) a diferença fica em torno de 0,1 na escala
0 a 10, e só aparece com habilidade individual ligada e um jogador de linha no gol.

Isto é **observável**, e a observação vale mais que o argumento: escale um jogador de linha no gol
com habilidade individual ligada e leia a coluna Gol na tabela de elenco.

### 12. A seção 4.4 descreve dois elencos e não diz qual se aplica

O bloco principal calcula a força de jogadores que já existem, e o parágrafo seguinte monta um
elenco do zero com 3 GOL, 4 LAT, 4 ZAG, 5 MEI, 4 ATA. Não está dito quando cada caminho vale.

**Resolução:** o caminho principal vale para clubes que trazem elenco no arquivo; o elenco sintético
é para clubes sem elenco. Nesta versão só o primeiro é implementado, e o segundo fica registrado
para quando existir clube sem dados.

### 13. "base/faixa" na 4.4 contra "teto/piso" na FORMAT-SPEC

Os mesmos números (div1 20 e 7, reputação 5 dá 22 e 7) aparecem como base e faixa na SIMULATION-SPEC
e como teto e piso na FORMAT-SPEC.

**Resolução:** vale a leitura da 4.4, porque a fórmula usa os dois de forma aditiva
(`força = nívelMapeado + base + rnd(3)`), o que não faz sentido para um teto. A FORMAT-SPEC descreve
o mesmo efeito por fora, olhando a faixa de valores que sai.

### 14. Não existe tabela de nível de país

A 4.4 escala a força pelo nível do país do clube (`nívelPaís <= 13` dispara multiplicadores de 0,40
a 0,75). Essa tabela está no código do jogo, não em nenhum arquivo de dados, então está fora do
alcance da regra clean-room.

**Resolução:** `nível` vira campo do país no conjunto de dados, e a tabela distribuída é derivada do
ranking mundial da FIFA por faixas: 1-5 dá 20, 6-10 dá 19, 11-20 dá 18, 21-30 dá 17, 31-40 dá 16,
41-55 dá 15, 56-70 dá 14, 71-90 dá 13, 91-110 dá 12, 111-130 dá 11, 131-150 dá 10, 151-170 dá 9,
171-190 dá 8, 191 ou pior dá 7.

Isto é **INFERIDO e uma divergência deliberada**: o original usa uma tabela fixa e o CLASSIC não vai
reproduzir esses multiplicadores até que ela seja observada. Como é dado e não lógica, trocar a
tabela depois não mexe em código nenhum.

### 15. O que significa "-4 se > 4" na entrada A da 4.2

A 4.2 diz que na criação do mundo `A` é o nível mapeado do clube, "-4 se > 4". O parêntese admite
mais de uma leitura.

**Resolução:** literal, `A = nívelMapeado - 4` quando `nívelMapeado > 4`, senão o próprio.

Na prática não há ambiguidade nenhuma: o nível do time vai de 6 a 20 (FORMAT-SPEC, campo `c`) e a
tabela de mapeamento devolve o próprio valor até 15, então o menor nível mapeado possível é 6. A
condição `> 4` é sempre verdadeira para qualquer clube que possa existir, e o ramo alternativo nunca
executa. Toda leitura do parêntese dá o mesmo resultado.

Vale notar que a leitura literal é **não monótona** se o ramo fosse alcançável: mapeado 4 daria
`A = 4` e mapeado 5 daria `A = 1`, um degrau para baixo. Como o ramo é inalcançável com os dados
reais, isso é só uma guarda defensiva. Existe um teste que fixa que níveis 6 a 20 sempre caem na
subtração, para que um conjunto de dados futuro com nível menor falhe alto em vez de produzir o
degrau em silêncio.

### 16. Desconto por temporada de chegada na temporada 1

O valor de mercado da 4.9 desconta por quando o jogador chegou ao clube, mas não diz o que vale para
quem já estava lá quando o mundo foi criado.

**Resolução:** contam como "mais antigo", sem desconto. Ninguém chegou por transferência antes da
primeira temporada existir.

### 17. De onde vem o talento de um profissional na criação do mundo

O talento (`es`, o campo `hash` do arquivo) existe em todo jogador, mas a única distribuição que a
spec dá está na seção 4.6, que trata da base. A 4.4 só fixa `es = 7 + rnd(4)` para o elenco
sintético, e não diz nada sobre os profissionais que vêm do arquivo.

**Resolução:** sortear pela mesma distribuição da 4.6, escolhida pela qualidade do clube. É a única
distribuição documentada, e nesta versão o valor é inerte de qualquer forma: todo efeito do talento
é condicionado a ter vindo da base, seja pelo `veio de base` do crescimento semanal, seja pelo
`desenvolvimento de base >= 60` do teto. Quando a base for implementada, este item precisa ser
revisto contra o que os arquivos realmente contêm.
