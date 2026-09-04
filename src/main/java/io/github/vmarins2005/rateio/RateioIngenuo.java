package io.github.vmarins2005.rateio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A versao ingenua, escrita de proposito: calcula a parte de cada um e arredonda.
 *
 * <p>E o que quase todo mundo escreve na primeira vez, e ela erra nos <b>dois</b> sentidos:
 *
 * <ul>
 *   <li><b>perde dinheiro</b> - R$ 100,00 entre tres da 33,33 para cada um, e a soma e
 *       R$ 99,99. Um centavo evaporou;</li>
 *   <li><b>inventa dinheiro</b> - R$ 0,05 entre dois da 0,025 para cada, que arredondado
 *       vira 0,03, e a soma e R$ 0,06. Um centavo apareceu do nada.</li>
 * </ul>
 *
 * <p>O segundo caso e o pior: um sistema que inventa dinheiro passa despercebido por muito
 * mais tempo que um que perde, porque ninguem reclama de receber a mais.
 *
 * <p>Nenhum modo de arredondamento resolve. {@code HALF_EVEN} reduz o vies mas nao garante
 * a soma; {@code DOWN} sempre perde; {@code UP} sempre inventa. O problema nao e o modo de
 * arredondamento - e arredondar cada parte isoladamente. Ver ADR 0002.
 */
public final class RateioIngenuo {

    private RateioIngenuo() {
    }

    public static Map<String, Dinheiro> distribuir(Dinheiro total, List<Participacao> participacoes,
                                                   RoundingMode arredondamento) {
        BigDecimal somaPesos = participacoes.stream()
                .map(participacao -> BigDecimal.valueOf(participacao.peso()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Dinheiro> partes = new LinkedHashMap<>();
        for (Participacao participacao : participacoes) {
            BigDecimal fracao = BigDecimal.valueOf(participacao.peso())
                    .divide(somaPesos, 10, RoundingMode.HALF_UP);
            BigDecimal parte = BigDecimal.valueOf(total.centavos())
                    .multiply(fracao)
                    .setScale(0, arredondamento);
            partes.put(participacao.id(), Dinheiro.deCentavos(parte.longValueExact()));
        }
        return partes;
    }
}
