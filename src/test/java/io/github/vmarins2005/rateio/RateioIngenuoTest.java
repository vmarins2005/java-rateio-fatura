package io.github.vmarins2005.rateio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes que <b>provam o problema</b>, e nao verificam uma solucao.
 *
 * <p>Eles passam quando o rateio ingenuo erra. Sem sentir isto, o metodo do maior resto
 * parece complicacao desnecessaria para uma divisao.
 */
class RateioIngenuoTest {

    private static final List<Participacao> TRES_IGUAIS =
            List.of(Participacao.de("a", 1), Participacao.de("b", 1), Participacao.de("c", 1));
    private static final List<Participacao> DOIS_IGUAIS =
            List.of(Participacao.de("a", 1), Participacao.de("b", 1));

    @Test
    @DisplayName("perde dinheiro: R$ 100,00 entre tres vira R$ 99,99")
    void perdeCentavo() {
        Map<String, Dinheiro> partes =
                RateioIngenuo.distribuir(Dinheiro.deReais("100.00"), TRES_IGUAIS, RoundingMode.HALF_UP);

        assertThat(soma(partes)).isEqualTo(Dinheiro.deReais("99.99"));
        assertThat(partes.values()).allMatch(parte -> parte.equals(Dinheiro.deReais("33.33")));
    }

    @Test
    @DisplayName("inventa dinheiro: R$ 0,05 entre dois vira R$ 0,06")
    void inventaCentavo() {
        Map<String, Dinheiro> partes =
                RateioIngenuo.distribuir(Dinheiro.deReais("0.05"), DOIS_IGUAIS, RoundingMode.HALF_UP);

        assertThat(soma(partes)).isEqualTo(Dinheiro.deReais("0.06"));
    }

    @Test
    @DisplayName("nenhum modo de arredondamento resolve: cada um erra de um jeito")
    void nenhumModoResolve() {
        Dinheiro cincoCentavos = Dinheiro.deReais("0.05");

        // HALF_EVEN reduz o vies estatistico e continua nao fechando a conta.
        assertThat(soma(RateioIngenuo.distribuir(cincoCentavos, DOIS_IGUAIS, RoundingMode.HALF_EVEN)))
                .isEqualTo(Dinheiro.deReais("0.04"));
        // DOWN sempre perde.
        assertThat(soma(RateioIngenuo.distribuir(cincoCentavos, DOIS_IGUAIS, RoundingMode.DOWN)))
                .isEqualTo(Dinheiro.deReais("0.04"));
        // UP sempre inventa.
        assertThat(soma(RateioIngenuo.distribuir(cincoCentavos, DOIS_IGUAIS, RoundingMode.UP)))
                .isEqualTo(Dinheiro.deReais("0.06"));

        // O problema nao e o modo de arredondamento: e arredondar cada parte isoladamente.
    }

    private static Dinheiro soma(Map<String, Dinheiro> partes) {
        return partes.values().stream().reduce(Dinheiro.ZERO, Dinheiro::somar);
    }
}
