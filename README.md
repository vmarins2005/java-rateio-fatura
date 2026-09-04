# Rateio de fatura — dividir dinheiro sem perder nem inventar centavo

Projeto de estudo de **property-based testing** e **mutation testing**, sobre um problema
que parece trivial e não é: dividir um valor entre participantes de forma que a soma das
partes seja exatamente o total.

Sexto de uma série em que cada repositório isola um conceito.

## O problema, antes da solução

`RateioIngenuoTest` roda a versão que quase todo mundo escreve na primeira vez — calcular
a parte de cada um e arredondar. Ela erra nos **dois** sentidos:

| Caso | Resultado | |
| --- | --- | --- |
| R$ 100,00 entre três (`HALF_UP`) | 33,33 × 3 = **R$ 99,99** | perde um centavo |
| R$ 0,05 entre dois (`HALF_UP`) | 0,03 × 2 = **R$ 0,06** | **inventa** um centavo |

Inventar é pior que perder: ninguém reclama de receber a mais, então o defeito sobrevive
muito mais tempo antes de alguém notar.

E nenhum modo de arredondamento resolve — `HALF_EVEN` dá R$ 0,04, `DOWN` sempre perde, `UP`
sempre inventa. O problema não é o modo: é **arredondar cada parte isoladamente**, tomando
uma decisão local para um resultado global.

## A solução

Método do maior resto, em três passos:

1. cada um recebe a parte inteira de `total × peso / somaDosPesos`;
2. sobra `total − soma das partes inteiras` centavos, sempre entre 0 e n−1;
3. a sobra vai, um centavo por vez, para quem tem o **maior resto** na divisão.

A soma fecha por construção, e ninguém fica a mais de um centavo do valor ideal.

O empate no resto é desfeito **por id**, e não pela ordem da lista — assim dois sistemas
que enviam os mesmos participantes em ordens diferentes produzem rateios idênticos.

## Como rodar

```bash
./mvnw test
```

28 testes — mas as 7 propriedades geram cerca de mil casos cada, então são milhares de
combinações por execução. Sem Maven instalado, sem banco, sem container. Só JDK 21.

Mutation testing (roda sob demanda, leva alguns minutos):

```bash
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```

Relatório em `target/pit-reports/index.html`.

## As propriedades

Exemplos escritos à mão cobrem o que você imaginou. Propriedades cobrem o que você não
imaginou — e é lá que moram os bugs de arredondamento.

| Propriedade | O que garante |
| --- | --- |
| `aSomaDasPartesEExatamenteOTotal` | não perde nem inventa dinheiro |
| `todaParteRecebeOPisoOuOPisoMaisUmCentavo` | ninguém fica a mais de um centavo do ideal |
| `embaralharAEntradaNaoMudaAParteDeNinguem` | determinismo independente da ordem |
| `multiplicarOPesoDeTodosNaoMudaOResultado` | peso é proporção |
| `comPesosIguaisAsPartesDiferemNoMaximoUmCentavo` | divisão igualitária é justa |
| `ratearZeroDaZeroParaTodos` | caso de borda |

A primeira sozinha seria satisfeita por uma implementação horrível — dar tudo ao primeiro
e zero ao resto também fecha a conta. É a segunda que impede isso. **Propriedade sozinha
quase nunca basta**, e escolhê-las é a parte difícil: estão registradas no ADR 0001, junto
com o que deliberadamente **não** é garantido.

## O que o mutation testing encontrou

Primeira execução: **89%**, quatro mutações sem cobertura, *test strength* 100% — tudo que
era exercitado estava verificado; o problema era código que nenhum teste tocava.

As quatro apontavam para `Dinheiro.subtrair`, `compareTo`, `emReais` e `toString`, e
tiveram destinos opostos:

- **`subtrair` e `compareTo` foram apagados.** Ninguém os usava. Eram métodos escritos por
  hábito — "todo value object de dinheiro tem" — e o relatório os expôs como código morto.
- **`emReais` e `toString` ganharam teste.** São a superfície de apresentação do tipo, o
  que vai para nota, tela e log.

Score depois: **100% (36 de 36)**, com cobertura de linha em 94%. Cobertura menor que
mutation score não é contradição — as linhas descobertas são construtores privados de
classes utilitárias, que não têm comportamento para mutar.

O relatório não disse qual caminho seguir. Disse onde olhar.

## Decisões registradas

| ADR | Assunto |
| --- | --- |
| [0000](docs/adr/0000-decisoes-base-do-projeto.md) | Centavos inteiros, peso em vez de percentual |
| [0001](docs/adr/0001-invariantes-escolhidas-para-o-rateio.md) | As invariantes — e o que não é garantido |
| [0002](docs/adr/0002-politica-de-arredondamento-e-destino-da-sobra.md) | Método do maior resto e destino da sobra |
| [0003](docs/adr/0003-meta-de-mutation-score-e-por-que-nao-100.md) | Meta de mutation score, e por que não 100 |

Duas decisões que valem discussão:

**Centavos inteiros em vez de `BigDecimal`** contradiz o projeto de DDD desta mesma série,
que usa `BigDecimal` — e está certo nos dois casos. A pergunta certa não é "qual tipo é
melhor para dinheiro", é "qual operação domina este código". Aqui é divisão com resto.

**Peso em vez de percentual** elimina um problema inteiro: com três participantes iguais,
não existe percentual com duas casas que some exatamente 100. Sistemas que exigem "a soma
dos percentuais deve dar 100" têm uma regra de negócio que é, na verdade, efeito colateral
da representação escolhida.

## Viés conhecido

O mesmo id ganha a sobra toda vez. Rateando R$ 100,00 entre três, doze vezes seguidas,
esse participante recebe doze centavos a mais no ano.

Compensar exigiria memória entre execuções, e o rateio deixaria de ser função pura. É
decisão de negócio, não técnica, e está registrada como não-garantia no ADR 0001.

## Exercícios

1. **Troque o desempate por "primeiro da lista"** e rode as propriedades. A de determinismo
   quebra, com o contraexemplo mínimo na tela. É property-based testing fazendo o trabalho
   que exemplo escrito à mão não faria.
2. **Implemente rateio com compensação de viés** — memória de quem já recebeu sobra. Depois
   escreva a propriedade que descreve a nova garantia, e o ADR que supera o 0001.
3. **Rode o PITest, abra o relatório HTML** e escolha uma mutação sobrevivente para matar.
   Se não sobrar nenhuma, adicione um `if` bobo ao algoritmo e veja o PITest encontrá-lo.
4. **Adicione rateio incremental** — participante entrando depois do rateio feito. Você vai
   descobrir que é outro problema, e que a solução atual não serve.

## Regras de trabalho neste repositório

- Regra nova de cálculo entra com a propriedade que a descreve, e não só com um exemplo.
- Método público sem uso é código morto: apagar, e não "testar para fechar a cobertura".
- Commits atômicos: cada commit compila e passa nos testes sozinho.

## O que eu faria diferente

_A preencher depois de usar._
