package io.github.vmarins2005.rateio;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Divide um valor entre participantes sem perder nem inventar centavo.
 *
 * <p>Metodo do maior resto (<i>largest remainder</i>), em tres passos:
 *
 * <ol>
 *   <li>cada um recebe a parte inteira de {@code total x peso / somaDosPesos};</li>
 *   <li>sobra {@code total - soma das partes inteiras} centavos, sempre entre 0 e n-1;</li>
 *   <li>a sobra vai, um centavo por vez, para quem tem o maior resto na divisao.</li>
 * </ol>
 *
 * <p>O passo 3 e o que garante a invariante central: a soma das partes e <b>exatamente</b>
 * igual ao total, por construcao. E como cada um recebe o piso ou o piso mais um centavo,
 * ninguem fica a mais de um centavo do valor ideal.
 *
 * <p>O desempate e por id, e nao pela ordem da lista - assim embaralhar a entrada nao muda
 * o resultado de ninguem. Ver ADR 0002.
 */
public final class Rateio {

    private Rateio() {
    }

    public static Map<String, Dinheiro> distribuir(Dinheiro total, List<Participacao> participacoes) {
        Objects.requireNonNull(total, "total e obrigatorio");
        exigirParticipacoesValidas(participacoes);

        BigInteger somaDosPesos = participacoes.stream()
                .map(participacao -> BigInteger.valueOf(participacao.peso()))
                .reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger totalEmCentavos = BigInteger.valueOf(total.centavos());

        List<Quinhao> quinhoes = new ArrayList<>(participacoes.size());
        long soma = 0;
        for (Participacao participacao : participacoes) {
            BigInteger[] divisao = totalEmCentavos
                    .multiply(BigInteger.valueOf(participacao.peso()))
                    .divideAndRemainder(somaDosPesos);
            long parteInteira = divisao[0].longValueExact();
            quinhoes.add(new Quinhao(participacao.id(), parteInteira, divisao[1]));
            soma += parteInteira;
        }

        distribuirSobra(quinhoes, total.centavos() - soma);

        Map<String, Dinheiro> partes = new LinkedHashMap<>();
        for (Participacao participacao : participacoes) {
            partes.put(participacao.id(), buscar(quinhoes, participacao.id()).comoDinheiro());
        }
        return partes;
    }

    public static Map<String, Dinheiro> igualmente(Dinheiro total, List<String> ids) {
        return distribuir(total, ids.stream().map(id -> Participacao.de(id, 1)).toList());
    }

    /**
     * Ordena por resto decrescente e, no empate, por id. Os {@code sobra} primeiros
     * recebem um centavo a mais.
     */
    private static void distribuirSobra(List<Quinhao> quinhoes, long sobra) {
        List<Quinhao> porResto = new ArrayList<>(quinhoes);
        porResto.sort(Comparator.comparing(Quinhao::resto).reversed().thenComparing(Quinhao::id));
        for (int i = 0; i < sobra; i++) {
            porResto.get(i).receberCentavo();
        }
    }

    private static Quinhao buscar(List<Quinhao> quinhoes, String id) {
        return quinhoes.stream()
                .filter(quinhao -> quinhao.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static void exigirParticipacoesValidas(List<Participacao> participacoes) {
        Objects.requireNonNull(participacoes, "participacoes sao obrigatorias");
        if (participacoes.isEmpty()) {
            throw new IllegalArgumentException("rateio precisa de ao menos um participante");
        }
        Set<String> ids = new HashSet<>();
        for (Participacao participacao : participacoes) {
            if (!ids.add(participacao.id())) {
                throw new IllegalArgumentException("participante repetido no rateio: " + participacao.id());
            }
        }
    }

    private static final class Quinhao {

        private final String id;
        private final BigInteger resto;
        private long centavos;

        private Quinhao(String id, long centavos, BigInteger resto) {
            this.id = id;
            this.centavos = centavos;
            this.resto = resto;
        }

        private void receberCentavo() {
            centavos++;
        }

        private String id() {
            return id;
        }

        private BigInteger resto() {
            return resto;
        }

        private Dinheiro comoDinheiro() {
            return Dinheiro.deCentavos(centavos);
        }
    }
}
