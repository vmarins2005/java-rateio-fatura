# ADR 0003 — Meta de mutation score, e por que não 100

Status: aceito · 2026-09-04 · supera: —

## Contexto

Cobertura de linha mede o que foi **executado**. Mutation score mede o que foi
**verificado**: o PITest altera o bytecode — troca `>` por `>=`, inverte condição, faz um
método retornar `null` — e verifica se algum teste reprova. Mutação que sobrevive é
comportamento que ninguém está checando.

Este projeto mostra a diferença em números medidos:

| Métrica | Valor |
| --- | --- |
| Cobertura de linha | 94% |
| Mutation score | 100% (36 de 36) |

Cobertura de linha menor que mutation score é comum e não é contradição: as linhas
descobertas são construtores privados de classes utilitárias, que não têm comportamento
para mutar.

## O que o PITest encontrou aqui

Na primeira execução, o score foi **89%**, com quatro mutações sem cobertura e *test
strength* de 100% — ou seja, tudo que era exercitado estava sendo verificado; o problema
era código que nenhum teste tocava.

As quatro mutações apontavam para `Dinheiro.subtrair`, `Dinheiro.compareTo`,
`Dinheiro.emReais` e `Dinheiro.toString`. Elas tiveram destinos opostos, e a diferença é a
lição:

- **`subtrair` e `compareTo` foram apagados.** Ninguém no projeto os usava. Eram métodos
  escritos por hábito — "todo value object de dinheiro tem" — e mutation testing os expôs
  como o que eram: código morto com custo de manutenção e zero valor.
- **`emReais` e `toString` ganharam teste.** São a superfície de apresentação do tipo — o
  que aparece em nota fiscal, em tela e em log — e um erro ali chega ao usuário final.

O relatório não disse qual caminho seguir; ele disse onde olhar. A decisão continua sendo
de quem escreve.

## Decisão

`mutationThreshold` em **95**, com o score atual em 100%.

Não 100, por duas razões:

1. **Mutantes equivalentes.** Existem mutações que alteram o bytecode sem alterar
   comportamento observável — trocar `<` por `<=` num limite que nunca é atingido, por
   exemplo. Nenhum teste honesto as mata. Com meta em 100, o primeiro mutante equivalente
   torna o build refém, e a saída fácil vira escrever um teste artificial só para matá-lo,
   ou desligar a verificação.
2. **A meta serve para impedir regressão, não para perseguir o número.** Cair de 100 para
   96 é ruído; cair para 80 é sinal de que alguém adicionou lógica sem teste.

Escopo: `io.github.vmarins2005.rateio.*`, que aqui é o projeto inteiro. Em um sistema real,
mutation testing roda no **domínio**, e não em controller, DTO ou configuração — o custo de
execução é alto e o retorno nessas camadas é baixo.

## Consequências

- \+ A meta pega regressão real sem transformar o número em objetivo.
- \+ O relatório aponta código morto, e não só teste faltando. Foi o achado mais valioso
  desta execução.
- − Mutation testing é lento: minutos em vez de segundos. Por isso **não** está no ciclo de
  `mvn test` — roda sob demanda, e num sistema real rodaria no pipeline noturno, e não em
  todo push.
- − Score alto não significa teste bom. Significa que o teste percebe mudanças de
  comportamento. Se as propriedades escolhidas forem fracas (ver ADR 0001), o score
  continua alto e o rateio continua errado — mutation testing verifica a força dos testes
  existentes, não se você testou a coisa certa.
