# ADR 0002 — Política de arredondamento e destino da sobra

Status: aceito · 2026-09-04 · supera: —

## Contexto

Dividir R$ 100,00 entre três pessoas não tem resposta exata em centavos. Alguém recebe um
centavo a mais. A decisão é **quem**, e por qual critério.

Antes disso, uma constatação que o `RateioIngenuoTest` demonstra rodando: **nenhum modo de
arredondamento resolve o problema.**

| Modo | R$ 0,05 entre dois | Resultado |
| --- | --- | --- |
| `HALF_UP` | 0,03 + 0,03 | R$ 0,06 — **inventa** um centavo |
| `HALF_EVEN` | 0,02 + 0,02 | R$ 0,04 — perde um centavo |
| `DOWN` | 0,02 + 0,02 | R$ 0,04 — sempre perde |
| `UP` | 0,03 + 0,03 | R$ 0,06 — sempre inventa |

E R$ 100,00 entre três com `HALF_UP` dá R$ 99,99.

Inventar dinheiro é pior que perder: ninguém reclama de receber a mais, então o defeito
sobrevive muito mais tempo antes de alguém notar.

O problema não é o modo de arredondamento. É **arredondar cada parte isoladamente**: cada
decisão é local, e a soma é global.

## Alternativas

**1. Arredondar cada parte e ajustar o último participante.**
Simples e comum. A conta fecha, e o último da lista vira lixeira de erro acumulado: com
muitos participantes, ele pode acabar vários centavos longe do valor ideal. Além disso,
"o último da lista" depende da ordem — o que quebra a invariante 4 do ADR 0001.

**2. Distribuir a sobra aleatoriamente.**
Justo estatisticamente, e impossível de auditar ou reproduzir. Rateio precisa dar o mesmo
resultado toda vez que rodar.

**3. Método do maior resto. — escolhida**

## Decisão

Cada participante recebe a parte inteira de `total × peso / somaDosPesos`. Sobra sempre
entre 0 e n-1 centavos, distribuídos um a um para quem tem o **maior resto** na divisão.

O critério é o mais defensável para quem recebe a menos: ganha o centavo quem chegou mais
perto de merecê-lo.

**Empate no resto é desfeito pelo id, e não pela ordem da lista.** É o que garante que dois
sistemas enviando os mesmos participantes em ordens diferentes produzam rateios idênticos.

Consequência aritmética: como cada um recebe o piso ou o piso mais um, ninguém fica a mais
de um centavo do ideal — a invariante 2 do ADR 0001 sai de graça.

## Consequências

- \+ A soma fecha por construção, sem ajuste posterior.
- \+ Determinístico e auditável: dá para explicar a um cliente por que ele recebeu um
  centavo a menos.
- \+ Não depende de modo de arredondamento — não há arredondamento no algoritmo.
- − **Viés sistemático em rateios repetidos.** O mesmo id ganha a sobra toda vez. Ao longo
  de um ano, ele recebe centavos a mais. Compensar exigiria memória entre execuções, e o
  rateio deixaria de ser função pura. Está registrado como não-garantia no ADR 0001.
- − O algoritmo é mais longo que uma divisão com arredondamento. É o preço de a conta
  fechar, e o `RateioIngenuoTest` existe justamente para justificar esse preço.
- − Requer todos os participantes de uma vez. Rateio incremental — participante entrando
  depois — é outro problema, e não está resolvido aqui.
