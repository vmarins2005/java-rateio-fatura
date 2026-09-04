# ADR 0001 — Invariantes escolhidas para o rateio

Status: aceito · 2026-09-04 · supera: —

## Contexto

Property-based testing só vale o que valem as propriedades escolhidas. Uma propriedade
fraca gera mil casos e não prova nada; uma propriedade errada trava o desenho.

Este ADR registra quais invariantes o rateio garante, e — mais importante — **quais
deliberadamente não garante**.

## As invariantes

### 1. A soma das partes é exatamente igual ao total

A razão de o projeto existir. Sem ela, dinheiro some ou aparece.

É a única propriedade que sozinha já seria satisfeita por uma implementação ruim: dar
tudo para o primeiro participante e zero para o resto fecha a conta. Por isso ela precisa
da propriedade 2 ao lado.

### 2. Cada um recebe o piso do valor ideal, ou o piso mais um centavo

Impede a implementação degenerada acima e garante que ninguém fica a mais de um centavo do
que deveria receber. Junto com a 1, esta é a definição de "rateio justo" neste domínio.

### 3. Nenhuma parte é negativa

Garantida pelo tipo `Dinheiro`, e testada mesmo assim: a propriedade documenta a intenção
para quem for mexer no algoritmo.

### 4. Embaralhar a entrada não muda a parte de ninguém

A que mais influenciou o desenho. É o motivo de o desempate ser **por id**, e não pela
ordem da lista.

Com desempate por posição, dois sistemas que enviam os mesmos participantes em ordens
diferentes produziriam rateios diferentes — e a divergência apareceria como um centavo de
diferença entre dois relatórios, que é exatamente o tipo de bug que consome uma semana.

### 5. Multiplicar o peso de todos pelo mesmo fator não muda o resultado

Peso é proporção. Se dobrar todos os pesos mudasse alguma parte, o conceito estaria mal
modelado.

### 6. Ratear zero dá zero para todos

Caso de borda trivial, e o primeiro a quebrar em implementações que dividem antes de
verificar.

## O que NÃO é garantido, de propósito

- **Que o mesmo participante receba a sobra em rateios sucessivos.** Ratear R$ 100,00 entre
  três, doze vezes seguidas, dá o centavo extra sempre para o mesmo id. Ao longo de um ano,
  esse participante recebe doze centavos a mais.

  Compensar isso exigiria memória entre execuções — o rateio deixaria de ser uma função
  pura e passaria a depender de histórico. É uma decisão de negócio, não técnica, e o dia
  em que ela aparecer merece ADR próprio.

- **Ordem estável na saída.** O `Map` devolvido preserva a ordem da entrada por
  conveniência, mas nada depende disso, e nenhum teste afirma isso.

- **Qualquer coisa sobre valores acima de `Long.MAX_VALUE` centavos.** O cálculo interno
  usa `BigInteger` para não estourar na multiplicação, mas o total é `long`.

## Consequências

- As seis propriedades cobrem, juntas, o espaço de entrada que importa: qualquer total até
  R$ 10 milhões, com 1 a 12 participantes e pesos de 1 a 10.000.
- A propriedade 4 é uma restrição de desenho, e não só um teste: mudar o critério de
  desempate para "primeiro da lista" a quebra imediatamente.
- Adicionar uma sétima propriedade é barato; remover uma exige justificar aqui.
