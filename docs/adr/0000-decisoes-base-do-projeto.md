# ADR 0000 — Decisões base do projeto

Status: aceito · 2026-09-04 · supera: —

## Contexto

O projeto resolve um problema pequeno e traiçoeiro: dividir um valor entre participantes
sem perder nem inventar centavo. É o tipo de código que parece trivial, é escrito em cinco
minutos e aparece na conferência contábil três meses depois.

O objetivo secundário é exercitar duas técnicas de teste que exemplos escritos à mão não
substituem: **property-based testing** e **mutation testing**.

## Decisões

### 1. Dinheiro em centavos inteiros, e não em `BigDecimal`

A conta central do problema — "dividir 10000 centavos em três partes e distribuir o resto"
— é aritmética inteira: divisão com resto, sem arredondamento e sem casa decimal escondida.

Com `BigDecimal`, a mesma conta exige escolher escala e modo de arredondamento a cada
passo, e a soma das partes passa a depender dessas escolhas. Com inteiros, a soma é igual
ao total **por construção**.

Isso contradiz a decisão do projeto de DDD da série, que usa `BigDecimal` — e está certo
nos dois casos. Lá o problema era representar valores com moeda e operações variadas; aqui
é dividir com resto. A pergunta certa não é "qual tipo é melhor para dinheiro", é "qual
operação domina este código".

O limite: centavo é a menor unidade. Domínios que precisam de fração de centavo — juros
diários, câmbio, combustível — precisam de outro tipo.

### 2. Peso, e não percentual

`Participacao` guarda peso inteiro. Percentual sofre de um problema que peso não tem: com
três participantes iguais, **não existe percentual com duas casas que some exatamente
100** — 33,33 três vezes dá 99,99; 33,34 três vezes dá 100,02.

Sistemas que exigem "a soma dos percentuais deve dar 100" acabam com uma regra de negócio
que é, na verdade, um efeito colateral da representação escolhida.

Com peso, cada um vale 1 e a soma dos pesos é o denominador. A pergunta some.
`Participacao.dePercentual` continua disponível para quem recebe percentual de fora — ele
só converte, sem exigir que some 100.

### 3. Sem framework, sem persistência

Só o cálculo e os testes. A suíte roda em menos de um segundo, o que muda o hábito de
rodar teste — e importa aqui, porque as propriedades geram cerca de mil casos cada.

### 4. A versão errada entra no repositório, e antes da certa

`RateioIngenuo` existe para ser executado e para falhar. O histórico do git mostra ele e o
teste que prova o erro entrando **antes** do método do maior resto.

Sem sentir o problema, o algoritmo parece complicação desnecessária para uma divisão.

## Consequências

- O repositório demonstra técnica de teste tanto quanto algoritmo.
- Não há como usar isto para juros ou câmbio sem trocar o tipo de dinheiro — e está dito.
