# Lacunas e ambiguidades da spec

Comportamento que a spec não determina de forma única. Cada item registra a **resolução adotada** no
código, para que a decisão fique visível em vez de escondida dentro de uma função.

Regra: o motor nunca adivinha em silêncio. Se você encontrar uma lacuna nova, acrescente aqui e abra
uma issue com o rótulo `spec-gap`.

## Seções 3.4 a 3.6

### 1. Divisor do duelo de chute nas temporadas 1 a 4

A seção 3.5 lista `D = 8` para as temporadas 1 a 4 sem dizer a qual família de duelo se aplica, e só
então dá 11 e 10 "a partir da temporada 5".

**Resolução (INFERIDO):** 8.0 para os três duelos antes da temporada 5. Guardado num único campo do `RuleSet`,
então uma correção na spec é uma linha.

### 2. Numeração de temporada

Não está dito se a contagem começa em 0 ou 1.

**Resolução (INFERIDO):** base 1. A compressão começa quando `temporada >= 5`.

### 3. O piso de 0.2 contra os pesos anti-exploit em 3.6b

Lendo literalmente, o piso roda **depois** da atribuição anti-exploit, então 0.10 e 0.05 viram ambos
0.2 e os dois casos ficam indistinguíveis.

**Resolução (INFERIDO):** manter a ordem literal. Existe um teste que afirma que os dois casos são iguais, para
que a suposição fique visível. Evidência a favor: sem o piso, uma defesa com um zagueiro sofreria
**mais** finalizações do que uma com zero, o que não faz sentido.

### 4. O `round` do anti-exploit em 3.6c

A spec escreve `round(wDef x 0.2)` sem dizer a escala.

**Resolução (INFERIDO):** `bfRound` para inteiro, como todo `round` da spec. Para um peso de defesa perto de
1.0 isso dá 0, que então bate no piso de 0.2.

### 5. Onde entra o bônus de marcação no meio-campo

A spec diz que a marcação soma 0 / 0.04 / 0.08, mas não diz se antes ou depois do divisor.

**Resolução (MEDIDO, `LineAggregatesTest`):** somado ao **total** do meio-campo, antes do divisor fixo. É a única leitura que
produz os 0.008 e 0.016 na escala 0 a 10 que a seção 3.12 cita. No caso degenerado de menos de três
meias o bônus não é aplicado.

### 6. Escala do arredondamento do goleiro fora de posição

`round(GK x 0.2)` pode ser inteiro ou decimal.

**Resolução (MEDIDO, `LineAggregatesTest`):** inteiro, na escala 0 a 10. É a única leitura que reproduz o exemplo da seção 5.3: um
jogador de linha com força 70 no gol rende 1.0 contra 7.0 de um goleiro de verdade. Consequência
aceita: força 40 no gol dá legitimamente 0.0, e o piso de 0.2 da resolução de chute cuida disso.

### 7. O que conta como zagueiro

Tanto o anti-exploit de 3.6b quanto o bônus de cabeceio do finalizador falam em "zagueiro" sem dizer
se é posição natural ou slot.

**Resolução (INFERIDO):** faixa de slots 3 a 8, coerente com a regra da seção 5.1 de que o motor agrupa por
slot e nunca por posição natural. As duas leituras só divergem para um jogador improvisado.

### 8. Elegibilidade do finalizador

"Sorteio ponderado entre os escalados (exceto goleiro)" não diz se o banco entra.

**Resolução (INFERIDO):** somente slots 2 a 25, ou seja, apenas quem está em campo. Reservas têm peso base
zero, mas os bônus por característica são aditivos, então sem o filtro um atacante do banco poderia
finalizar.

### 9. Os números de alavanca da seção 3.16 não fecham

A seção 3.16 afirma que 20 pontos de diferença de força no meio-campo levam o duelo de posse de 55%
para "~69%" e o de chance de 50% para "~56%".

O primeiro não reproduz: a fórmula de 3.6a dá **67,07%** com divisor 8 e 63,84% com divisor 11.
O segundo não reproduz de jeito nenhum, porque **o duelo de chance não lê o meio-campo** - ele
compara ataque contra defesa.

Dos outros números de 3.16, três reproduzem exatamente em qualquer escalação: 0.614, 0.55 e 9,81%.
Os dois de conversão, 8,8% e 11,1%, reproduzem só de forma aproximada: o valor medido é 8,71% e
11,09%, e a diferença vem da alavanca anti-goleada da 3.6c, que a 3.16 não leva em conta. Os quatro
restantes - 0.565, 0.50, ~16 chutes e ~12,6 chutes - só reproduzem quando a linha de defesa e a linha
de ataque preenchem exatamente os divisores fixos da 3.4 (5 defensores, 3 atacantes), e mesmo assim os
volumes de chute só fecham depois de também recontar as posses como 47 em vez de 46, que é o item 28.
Num 4-4-2, a formação que a IA mais escolhe, essas duas linhas ficam desiguais e os quatro números não
fecham; ver itens 28 e 30.

**Resolução (MEDIDO, `DuelsTest`, `ShotResolutionTest`, `SanityCheckTest`):** tratar esse parágrafo
como narrativo. Testar apenas os valores exatos. Item aberto na spec.

## Seção 4 - criação do mundo

### 10. A spec não define ordem de sorteio na criação do mundo

A seção 0 diz que o original cria um gerador novo, sem semente, a cada sorteio. Não existe ordem a
imitar, e nenhuma seção descreve a criação do mundo como uma sequência.

**Resolução (INFERIDO):** definimos a nossa. A semente deriva por posição no mundo, nunca por contagem de
sorteios: `raiz -> WORLDGEN -> clube -> jogador`, com a chave do clube vinda do `fileRef`. Dentro de
um jogador os sorteios são sequenciais, na ordem estilo, força, atributos, bônus de característica,
contrato. Isso torna irrelevante a ordem em que os clubes são gerados, que é o que permite gerar em
paralelo sem mudar resultado.

### 11. Três linhas da tabela 4.2 não dão fórmula para Gol

Lateral ofensivo, volante e meia armador listam seis atributos. Falta **Gol** nos três. As outras
quatro linhas dão os sete, e toda linha de jogador de linha usa o mesmo idioma: `Gol = 1+rnd(k)`,
com k igual a 4 no lateral defensivo, 7 no zagueiro e 6 no atacante.

**Resolução (INFERIDO):** `Gol = 1+rnd(4)` nas três linhas omissas.

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

**Resolução (INFERIDO):** o caminho principal vale para clubes que trazem elenco no arquivo; o elenco sintético
é para clubes sem elenco. Nesta versão só o primeiro é implementado, e o segundo fica registrado
para quando existir clube sem dados.

### 13. "base/faixa" na 4.4 contra "teto/piso" na FORMAT-SPEC

Os mesmos números (div1 20 e 7, reputação 5 dá 22 e 7) aparecem como base e faixa na SIMULATION-SPEC
e como teto e piso na FORMAT-SPEC.

**Resolução (INFERIDO):** vale a leitura da 4.4, porque a fórmula usa os dois de forma aditiva
(`força = nívelMapeado + base + rnd(3)`), o que não faz sentido para um teto. A FORMAT-SPEC descreve
o mesmo efeito por fora, olhando a faixa de valores que sai.

### 14. Não existe tabela de nível de país

A 4.4 escala a força pelo nível do país do clube (`nívelPaís <= 13` dispara multiplicadores de 0,40
a 0,75). Essa tabela está no código do jogo, não em nenhum arquivo de dados, então está fora do
alcance da regra clean-room.

**Resolução (INFERIDO):** `nível` vira campo do país no conjunto de dados, e o importador o **deriva dos
próprios dados**: o nível do clube mais forte que o país tem.

Esta resolução substitui uma anterior, que derivava o nível do ranking mundial da FIFA por faixas. A
derivação a partir dos dados é melhor por três motivos. Não depende de fonte externa nenhuma, então
não há licença a respeitar nem número inventado de memória. Fica na mesma escala que o nível de
clube, que é com o que a 4.4 compara. E é reproduzível: qualquer pessoa com a mesma instalação chega
na mesma tabela.

A derivação se valida sozinha no conjunto distribuído: os cinco países que ela classifica no topo
(nível 20) são exatamente os cinco que a 4.8 paga mais, uma tabela que a derivação não consulta. A
distribuição cai suavemente de 20 até 11, e só 23 dos 134 países ficam em 13 ou menos, cobrindo 34
dos 703 clubes.

Um país sem nenhum clube não tem como ser avaliado e cai num valor acima do limiar, para que a falta
de dado não enfraqueça um elenco em silêncio.

Continua sendo uma **divergência deliberada**: o original usa uma tabela própria e o CLASSIC não vai
reproduzir esses multiplicadores até que ela seja observada. Como é dado e não lógica, trocar a
tabela depois não mexe em código nenhum.

### 15. O que significa "-4 se > 4" na entrada A da 4.2

A 4.2 diz que na criação do mundo `A` é o nível mapeado do clube, "-4 se > 4". O parêntese admite
mais de uma leitura.

**Resolução (INFERIDO):** literal, `A = nívelMapeado - 4` quando `nívelMapeado > 4`, senão o próprio.

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

**Resolução (INFERIDO):** contam como "mais antigo", sem desconto. Ninguém chegou por transferência antes da
primeira temporada existir.

### 17. De onde vem o talento de um profissional na criação do mundo

O talento (`es`, o campo `hash` do arquivo) existe em todo jogador, mas a única distribuição que a
spec dá está na seção 4.6, que trata da base. A 4.4 só fixa `es = 7 + rnd(4)` para o elenco
sintético, e não diz nada sobre os profissionais que vêm do arquivo.

**Resolução (INFERIDO):** sortear pela mesma distribuição da 4.6, escolhida pela qualidade do clube. É a única
distribuição documentada, e nesta versão o valor é inerte de qualquer forma: todo efeito do talento
é condicionado a ter vindo da base, seja pelo `veio de base` do crescimento semanal, seja pelo
`desenvolvimento de base >= 60` do teto. Quando a base for implementada, este item precisa ser
revisto contra o que os arquivos realmente contêm.

### 18. A tabela de reputação da 4.4 não cobre reputação zero

A 4.4 lista as faixas por reputação de 5 até 1. A reputação vai de 0 a 5 (seção 5.5), então falta
uma linha.

**Resolução (INFERIDO):** reputação 0 usa a mesma faixa de 1, 2 e 3, ou seja base 5 e faixa 1. As três
reputações mais baixas já são indistinguíveis na tabela, então estender a menor para o zero não
inventa comportamento novo, só fecha o buraco. Há um teste que fixa que 0, 1, 2 e 3 dão o mesmo
resultado, para que a suposição fique visível.

Vale notar que este caminho só vale para seleções. Um clube em liga escolhe pela divisão e nunca lê
a reputação aqui.

### 19. A cadeia do lateral na 4.3 não tem padrão

A 4.3 fecha as cadeias do meia e do atacante com um padrão explícito, mas a do lateral termina numa
condição: "1 se Velocidade/Cruzamento; 0 se Desarme/Marcação; senão 1 se Drible/Finalização/Passe/
Armação". Um lateral que não casa com nenhuma das três fica sem estilo. As características de
jogador de linha vão de 4 a 13, e as duas que ficam de fora de todos os testes são Cabeceio e
Resistência, então um lateral com essas duas cai no vazio.

**Resolução (INFERIDO):** 0, defensivo. A última cláusula é condicional e entrega 1, então não casar com ela
significa não ser 1. O meia e o atacante recebem padrão 1 porque a spec diz isso explicitamente para
eles; o lateral não tem essa frase.

### 20. Uma fórmula para vários atributos: um sorteio ou um por atributo

A 4.2 escreve a mesma fórmula para mais de um atributo em dois lugares. Na linha do goleiro,
`Des/Arm/Fin = B+rnd(3)`. Na lista de bônus, `Armação -> Arm e Pas +B+rnd(5)`. Nos dois casos não
está dito se o `rnd` é sorteado uma vez e usado nos dois ou três atributos, ou uma vez por atributo.

**Resolução (INFERIDO):** um sorteio por atributo, nos dois casos.

O argumento é o efeito visível. Com sorteio único, todo goleiro do jogo sairia com desarme,
armação e finalização exatamente iguais entre si, e todo jogador com a característica Armação
teria o mesmo incremento em armação e passe. Isso seria um padrão perceptível na tabela de elenco,
e nada na spec sugere que ele exista. A notação compacta economiza três linhas de tabela, o que
explica a escrita sem implicar sorteio compartilhado.

Há testes que fixam as duas escolhas, então se a observação contradisser, o que muda é um teste
e uma linha.

### 21. A spec nomeia os cinco países que pagam mais, mas não publica os cinco índices

A 4.8 dá uma tabela de salário melhor para clubes de {ALE, FRA, ITA, ING, ESP}. A FORMAT-SPEC
publica o índice numérico de quatro deles (3 Alemanha, 65 Espanha, 72 França, 104 Itália) e não o da
Inglaterra. A tabela completa de 224 países está no arquivo `countries.json`, que não acompanha a
spec.

**Resolução (INFERIDO):** o índice da Inglaterra é **97**, e isto veio dos dados, não de chute.

A convenção de nome de arquivo marca cada clube com o país (`1deagosto_ang`, `barcelona_esp`), então
o sufixo dá o código do país de cada índice. O sufixo `ing` aparece no índice 97. E os índices
publicados pela FORMAT-SPEC estão em ordem alfabética portuguesa (3 Alemanha, 5 Angola, 11 Argentina,
21 Bélgica, 29 Brasil, 65 Espanha, 72 França, 104 Itália, 154 Portugal), o que coloca 97 exatamente
onde Inglaterra pertence, entre França e Itália.

O conjunto é `{3, 65, 72, 97, 104}`. Evidência independente: a derivação de nível de país do item 14,
que não consulta esta lista, classifica exatamente esses cinco no topo.

O campo `majorLeague` do país continua existindo, porque o mesmo conjunto reaparece na 4.5 (bônus
continental de crescimento) e parcialmente na 4.10 (limiares de topMundial), e porque um conjunto de
dados que não venha de uma instalação precisa poder dizer isso por conta própria.

### 22. Onde entram os multiplicadores da 4.9

O bloco da 4.9 lista os multiplicadores (estrela, topMundial, atacante, titular) entre a definição
de `baseNível` e o termo de idade, mas a linha que produz o resultado, `valor = quadrático x
baseNível`, vem depois. O texto não diz sobre o que eles incidem.

A escolha muda o número. Um titular estrela de força 50, 24 anos, clube nível 20:
- multiplicando o `baseNível` antes do termo de idade: `600 x 1,7 = 1020`, mais 176, dá 11,96 M.
- multiplicando o valor pronto: `(600 + 176) x 1,7`, dá 13,19 M.

**Resolução (INFERIDO):** incidem sobre o valor pronto, depois de o `baseNível` estar completo.

A aferição da própria spec (`100^2 x (600+176)`) prova que o termo de idade entra **dentro** do
`baseNível`, então o `baseNível` está fechado antes de qualquer multiplicação. E os descontos por
temporada de chegada, que vêm logo abaixo na mesma lista e claramente incidem sobre o valor, dão o
padrão de leitura para os multiplicadores acima deles.

Como todos são multiplicações, a ordem entre eles não importa; só importa estarem depois da soma.
Há um arredondamento único no fim, então nem a ordem entre eles muda o centavo.

### 23. Atributos individuais são sempre gerados, mesmo com a opção desligada

A FORMAT-SPEC diz, como CONFIRMADO, que o original só gera os sete atributos individuais quando a
opção `habilidadeIndividual` está ligada. Com ela desligada o jogador tem só a força.

**Resolução (INFERIDO):** geramos sempre. Custa sete sorteios e não é observável com a opção desligada, porque
nesse modo o motor lê a força e nunca olha os atributos. Em troca, o jogador é o mesmo jogador
independentemente de como a opção está, o que evita que ligar a opção no meio de uma carreira mude
quem cada um é.

Isto é uma simplificação deliberada, não uma leitura da spec. Se algum dia a opção puder ser trocada
com efeito visível, este item vira uma decisão de verdade.

### 24. Nenhum arquivo diz em qual divisão cada clube joga

O arquivo do time não tem campo de divisão. O arquivo de configuração da liga nacional descreve a
forma da pirâmide (`pais`, `divisao`, `nTimes`, `nRebaixados`) e **não tem lista de times**. Ou seja,
a associação clube-divisão não está em nenhum dado distribuído, e a divisão escolhe a base de força
da 4.4 (20 na primeira divisão contra 1 fora da pirâmide), então errar isso muda todo jogador.

**Resolução (INFERIDO):** ordenar os clubes de cada país por nível, decrescente, e preencher cada divisão na
ordem até o `nTimes` dela.

A evidência é forte e verificável: no conjunto distribuído, os vinte clubes brasileiros de maior
nível são exatamente os vinte que disputaram a Série A de 2022, e os vinte seguintes são exatamente
os da Série B. O corte de nível cai exatamente na fronteira (níveis 19, 18, 17 e 16 somam vinte
clubes), então nem empate houve ali.

Empates são desfeitos pela referência do arquivo. Nas divisões de baixo os níveis se repetem muito
(cinquenta clubes brasileiros no nível 7), e sem critério fixo a mesma base de dados geraria
pirâmides diferentes a cada execução, quebrando toda semente já compartilhada. O critério é
arbitrário e está registrado como tal.

Isto é **observável**: basta abrir o jogo e ver quem está em cada divisão. Uma observação que
contrarie a ordenação por nível derruba esta resolução, e o custo é uma função.

### 25. A distribuição de talento nos arquivos não é a da seção 4.6

A 4.6 dá distribuições de talento por qualidade do clube, com pico em 5 e 6 (25% a 35% cada). O
campo `hash` dos 703 arquivos distribuídos é quase **uniforme**: cada valor de 1 a 10 aparece entre
1400 e 1800 vezes, e o 0 aparece 186 vezes.

**Resolução (INFERIDO):** as duas coisas não estão em conflito, e o item 17 partia de uma premissa errada. As
distribuições da 4.6 descrevem a **geração de um júnior novo em tempo de execução**, não o conteúdo
dos arquivos. Um profissional importado traz o talento dele no arquivo e nada precisa ser sorteado.

O item 17 fica revisto: só há sorteio quando a base passar a gerar jogadores, e aí a distribuição da
4.6 é que vale.

### 26. Nenhum arquivo diz o continente de um país

A 3.3 usa o continente do clube no deságio do Mundial de Clubes, e a 4.9 usa a nacionalidade europeia
num degrau de valor de mercado. Nenhum arquivo distribuído tem campo de continente, e a tabela está
no código do jogo.

**Resolução (INFERIDO):** por enquanto o importador grava um continente que não é a Europa, e registra isso.

O motivo de não inventar a tabela agora é que ela é inerte: o Mundial de Clubes precisa de
competições, que não existem, e o degrau de valor da 4.9 precisa de clube de nível 21 ou mais, que
nenhum arquivo expressa. Escolher "não Europa" garante que a falta de dado não conceda isenção
europeia a ninguém, que é o erro que passaria despercebido.

Quando as competições chegarem, isto deixa de ser inerte e precisa de tabela de verdade.

### 27. Um país sem arquivo de liga deixa todos os seus clubes fora de qualquer divisão

O item 24 resolve **como** ordenar os clubes de um país dentro da pirâmide dele. Não trata do caso
em que não há pirâmide nenhuma, que na instalação distribuída é o caso dominante e não a exceção:
`conf_ligas_nacionais/` traz apenas `BRA.cfg` e `ESP.cfg`, então **533 dos 703 clubes ficam sem
divisão**.

Isso não é um detalhe de calibragem. A divisão entra em três lugares e todos empurram na mesma
direção:

| Onde | Divisão 1 | Sem divisão |
|---|---|---|
| Base de força da 4.4 | 20 | 1 |
| Teto de crescimento da 4.5 | 80 a 100 por reputação | 30 |
| Piso de declínio da 4.5 | 35 | 1 |

O efeito já é visível num mundo gerado. Bayern e Real Madrid têm o mesmo nível 20 nos arquivos, ou
seja, os dados dizem que são equivalentes:

```
realmadrid_esp   div 1     melhor 67  Benzema
liverpool_ing    div nula  melhor 47  Salah
bayern_ale       div nula  melhor 45  Neuer
juventus_it      div nula  melhor 36  Szczesny
milan_it         div nula  melhor 33  Calabria
```

E a parte que ainda não aparece é pior que essa. Quando a evolução semanal da 4.5 entrar, o Neuer
com 45 já está acima do teto de 30 do `div0`, logo nunca cresce, e declina rumo ao piso de 1.
O Benzema cresce rumo a 100 e nunca cai abaixo de 35. Em poucas temporadas todo país sem arquivo de
liga desaba, o que o jogo original claramente não faz.

**Resolução atual (EM ABERTO):** nenhuma. O importador registra uma nota dizendo quantos clubes ficaram sem
divisão, e a nota subestima o problema porque só menciona a base de geração da 4.4.

**Hipótese a testar:** os dois `.cfg` são configurações **sobreponíveis** pelo usuário, colocadas por
cima de uma tabela de ligas embutida no jogo, e não o conjunto completo das ligas. Isso explicaria ao
mesmo tempo por que só dois arquivos são distribuídos e por que o Bayern não é fraco no jogo real.
A tabela embutida ficaria no código, fora do alcance da regra clean-room, exatamente como a tabela de
nível de país do item 14.

**Como decidir:** é observável. Basta abrir o original e ver se a Alemanha tem liga com divisões, e
quantas. Se tiver, o recurso não pode ser `div0`, e a saída provável é derivar uma pirâmide sintética
para país não configurado a partir do nível dos clubes, em vez de jogar todos na faixa mais fraca.

Enquanto isso não for decidido, qualquer aferição estatística feita fora do Brasil e da Espanha mede
este item e não o motor.

### 28. A contagem de tiques da 3.1 não bate com a da 3.16

A 3.1 dá `extra1 = rand(0..2)` e `extra2 = rand(1..5)`, com o primeiro tempo nos minutos
`0..44+extra1` e o segundo em `0..44+extra2`. Isso dá `45+extra1` mais `45+extra2` tiques, ou seja
de 91 a 97, que é exatamente a faixa que a própria 3.1 afirma. A média é `45+1+45+3 = 94`, logo
cada time é possuidor 47 vezes.

A 3.16 fala em cerca de 92 tiques e cerca de 46 posses por time, e os números derivados dela são
aritmética sobre 46, e não sobre 47:

```
chutes do mandante   46 x 0,614 x 0,565 = 15,96   a spec diz "~16 chutes"
chutes do visitante  46 x 0,55  x 0,50  = 12,65   a spec diz "~12,6 chutes"
```

Com 47 o número do visitante seria 12,93, que teria sido escrito 12,9. Ou seja, a 3.16 foi calculada
com 92 enquanto a 3.1 produz 94.

**Resolução (MEDIDO, `SanityCheckTest`):** implementar a 3.1 ao pé da letra, porque a faixa de 91 a 97 que ela declara é
consistente com as fórmulas dela mesma, e as cifras com til da 3.16 não são. A validação deriva a
contagem esperada de chutes da posse **medida**, nunca de um 46 fixo. É o mesmo tratamento que o
item 9 já dá ao parágrafo de alavanca da 3.16.

Isto é **observável**: contar os minutos de uma partida no jogo original resolve.

### 29. A posse exibida não chega aos 55/45 da 3.16

A 3.5 diz que a porcentagem exibida vem de um contador separado de vitórias no duelo de posse.
Contando o vencedor do duelo a cada tique, o mandante fica, em 92 tiques:

```
46 x 0,614 + 46 x (1 - 0,55) = 28,24 + 20,70 = 48,94 de 92 = 53,2 por cento
```

A 3.16 diz cerca de 55/45.

**Resolução (MEDIDO, `SanityCheckTest`):** contar o vencedor do duelo a cada tique, que é a única leitura que a frase da 3.5
admite. A validação verifica uma faixa que contém 53,2 e exclui 50,0 e 60,0, e registra o valor
medido, para que uma correção futura da spec tenha com o que comparar.

A diferença é pequena e pode ser só arredondamento generoso da 3.16, mas registrar é mais barato
que redescobrir.

### 30. Os volumes de chute da 3.16 só aparecem com todas as linhas no divisor

A 3.16 dá `P(chute | posse)` de 0,565 para o mandante e 0,50 para o visitante, e deriva daí os
"~16 chutes" e os "~12,6 chutes". O 0,50 do visitante só sai se `ATAQUE(TB)` e `DEFESA(OPP)` forem
iguais, porque o duelo de chance da 3.6b compara **essas duas linhas** e não um time contra o outro.
Dois times equivalentes não bastam: as duas grandezas comparadas vêm de linhas diferentes. É a mesma
leitura que o item 9 já registrou ao notar que o duelo de chance não lê o meio-campo.

Com os divisores fixos da 3.4 (5 para a defesa, 5 para o meio, 3 para o ataque), a igualdade exige
5 defensores e 3 atacantes. Num 4-4-2, que é a formação que a IA mais escolhe, sobram 4 defensores e
2 atacantes, e com força 50 dos dois lados (nota 4,8 por jogador, já com o multiplicador 0,95 da liga
nacional para reputação 3) as linhas ficam desiguais:

```
DEFESA = 4 x 4,8 / 5 = 3,84      ATAQUE = 2 x 4,8 / 3 = 3,20

visitante  wA = 1 + (3,20 - 3,84)/8 = 0,92   wD = 1,08   P(chute) = 0,92/2,00 = 0,460
mandante   wA = 0,92 + 0,3        = 1,22   wD = 1,08   P(chute) = 1,22/2,30 = 0,530
```

Com 5 defensores e 3 atacantes as duas linhas valem 4,8, a diferença zera e voltam os 0,50 e 0,565
da 3.16. Medido em 20000 partidas com semente fixa, temporada 1, campo normal:

| Grandeza | 4-4-2 | Linhas no divisor | 3.16 |
|---|---|---|---|
| Chutes do mandante | 15,31 | 16,32 | ~16 |
| Chutes do visitante | 11,93 | 12,96 | ~12,6 |
| Gols do mandante | 1,333 | 1,421 | ~1,4 |
| Gols do visitante | 1,324 | 1,435 | ~1,4 |

"Linhas no divisor" é a única escalação de onze jogadores que põe defesa e ataque exatamente sobre os
seus divisores: 5 defensores, 3 atacantes e, por consequência, 2 meias. Ela reproduz a 3.16 ao
centésimo, o que localiza a diferença na escalação e não na montagem do motor. O meio-campo fica
desfalcado e cai para a nota degenerada de 0,01, o que não muda nada aqui porque acontece dos dois
lados e o duelo de posse lê só a diferença.

Note ainda que os "~12,6" da 3.16 são aritmética sobre 46 posses. Sobre as 47 que a 3.1 produz, a
mesma conta dá 12,92, que é o valor medido. O item 28 e este se somam.

O resto da 3.16 bate exatamente nas duas escalações: 0,614 e 0,55 no duelo de posse, e 8,71 por cento
contra 11,09 por cento na conversão, ante os 8,8 e 11,1 da 3.15. Ou seja, o mando invertido está
reproduzido; o que não reproduz é só o volume.

**Resolução (MEDIDO, `SanityCheckTest`):** tratar os volumes de chute da 3.16 como calculados sobre uma escalação com todas as
linhas cheias, e não sobre uma escalação real. A validação afirma as duas coisas: as faixas medidas
do 4-4-2, que é o caso que o jogo produz, e a reprodução exata da 3.16 com as linhas no divisor, que
é a prova de que a diferença vem do defeito 3 da 3.15 (divisores fixos) e não de um erro de
montagem. Nenhuma faixa foi alargada para acomodar a 3.16.

Isto é **observável**: ler a média de chutes de uma temporada IA contra IA no jogo original, junto
com a formação escalada, resolve.

### 31. A partir de qual minuto do tempo se conta o desgaste de 7 em 7 minutos da 3.9

A 3.9 diz que o desgaste acontece "a cada 7 minutos" e dá "~7 descontos por tempo", mas não diz se a
contagem começa no primeiro minuto do tempo ou no sétimo. A seção também afirma que um jogador de 24
anos "perde ~28 de energia por partida completa".

Um jogador de 24 anos cai na faixa `<=25 -> 2`, então perde 2 por desconto. 28 de energia implica 14
descontos na partida inteira, ou seja 7 por tempo, não 6.

Contando os minutos de um tempo de 45 a partir de zero, os descontos caem em 0, 7, 14, 21, 28, 35 e
42, o que dá exatamente 7 descontos. Contando a partir de um, os descontos cairiam em 7, 14, 21, 28,
35 e 42, o que dá 6.

`7 descontos x 2 de custo = 14 energia por tempo x 2 tempos = 28`, batendo com a spec. `6 x 2 = 12 x
2 = 24`, que não bate.

**Resolução (INFERIDO):** contar os minutos de cada tempo a partir de zero, reiniciando a contagem no
início do segundo tempo. É a única leitura das duas que reproduz os ~28 de energia que a 3.9 cita
para um jogador de 24 anos numa partida completa. Testado em `EnergyTest`.
