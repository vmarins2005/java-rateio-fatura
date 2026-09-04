package io.github.vmarins2005.rateio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RateioTest {

    @Nested
    @DisplayName("divisao igualitaria")
    class DivisaoIgualitaria {

        @Test
        @DisplayName("R$ 100,00 entre tres: a sobra de um centavo vai para um so, e a soma fecha")
        void cemEntreTres() {
            Map<String, Dinheiro> partes = Rateio.igualmente(Dinheiro.deReais("100.00"), List.of("a", "b", "c"));

            assertThat(partes).containsExactlyInAnyOrderEntriesOf(Map.of(
                    "a", Dinheiro.deReais("33.34"),
                    "b", Dinheiro.deReais("33.33"),
                    "c", Dinheiro.deReais("33.33")));
            assertThat(soma(partes)).isEqualTo(Dinheiro.deReais("100.00"));
        }

        @Test
        @DisplayName("R$ 0,05 entre dois: 3 e 2 centavos, e nao 3 e 3")
        void cincoCentavosEntreDois() {
            Map<String, Dinheiro> partes = Rateio.igualmente(Dinheiro.deReais("0.05"), List.of("a", "b"));

            assertThat(partes.get("a")).isEqualTo(Dinheiro.deCentavos(3));
            assertThat(partes.get("b")).isEqualTo(Dinheiro.deCentavos(2));
            assertThat(soma(partes)).isEqualTo(Dinheiro.deReais("0.05"));
        }

        @Test
        @DisplayName("um centavo entre tres: alguem leva tudo, e ninguem leva fracao")
        void umCentavoEntreTres() {
            Map<String, Dinheiro> partes = Rateio.igualmente(Dinheiro.deCentavos(1), List.of("a", "b", "c"));

            assertThat(soma(partes)).isEqualTo(Dinheiro.deCentavos(1));
            assertThat(partes.values().stream().filter(parte -> !parte.ehZero())).hasSize(1);
        }
    }

    @Nested
    @DisplayName("pesos e percentuais")
    class PesosEPercentuais {

        @Test
        @DisplayName("pesos que dividem exato nao geram sobra")
        void divisaoExata() {
            Map<String, Dinheiro> partes = Rateio.distribuir(Dinheiro.deReais("100.00"), List.of(
                    Participacao.de("a", 50), Participacao.de("b", 30), Participacao.de("c", 20)));

            assertThat(partes).containsExactlyInAnyOrderEntriesOf(Map.of(
                    "a", Dinheiro.deReais("50.00"),
                    "b", Dinheiro.deReais("30.00"),
                    "c", Dinheiro.deReais("20.00")));
        }

        @Test
        @DisplayName("a sobra vai para quem tem o maior resto, e nao para o primeiro da lista")
        void sobraVaiParaOMaiorResto() {
            // 10 centavos com pesos 1, 2 e 3 (soma 6):
            //   a: 10x1/6 = 1 resto 4   <- maior resto
            //   b: 10x2/6 = 3 resto 2
            //   c: 10x3/6 = 5 resto 0
            // soma das partes inteiras = 9, sobra 1 centavo, que vai para "a".
            Map<String, Dinheiro> partes = Rateio.distribuir(Dinheiro.deCentavos(10), List.of(
                    Participacao.de("a", 1), Participacao.de("b", 2), Participacao.de("c", 3)));

            assertThat(partes.get("a")).isEqualTo(Dinheiro.deCentavos(2));
            assertThat(partes.get("b")).isEqualTo(Dinheiro.deCentavos(3));
            assertThat(partes.get("c")).isEqualTo(Dinheiro.deCentavos(5));
        }

        @Test
        @DisplayName("percentual com duas casas vira peso, sem exigir que a soma de 100")
        void percentualViraPeso() {
            Map<String, Dinheiro> partes = Rateio.distribuir(Dinheiro.deReais("100.00"), List.of(
                    Participacao.dePercentual("a", "33.33"),
                    Participacao.dePercentual("b", "33.33"),
                    Participacao.dePercentual("c", "33.33")));

            // Os percentuais somam 99,99 e o rateio continua fechando em 100,00.
            assertThat(soma(partes)).isEqualTo(Dinheiro.deReais("100.00"));
        }
    }

    @Nested
    @DisplayName("determinismo")
    class Determinismo {

        @Test
        @DisplayName("embaralhar a lista nao muda a parte de ninguem")
        void ordemNaoImporta() {
            List<Participacao> participacoes = List.of(
                    Participacao.de("a", 1), Participacao.de("b", 1), Participacao.de("c", 1));
            List<Participacao> invertida = new ArrayList<>(participacoes);
            java.util.Collections.reverse(invertida);

            Map<String, Dinheiro> original = Rateio.distribuir(Dinheiro.deReais("100.00"), participacoes);
            Map<String, Dinheiro> embaralhada = Rateio.distribuir(Dinheiro.deReais("100.00"), invertida);

            assertThat(embaralhada).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("entradas invalidas")
    class EntradasInvalidas {

        @Test
        @DisplayName("lista vazia e recusada")
        void listaVazia() {
            assertThatThrownBy(() -> Rateio.distribuir(Dinheiro.deReais("10.00"), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ao menos um participante");
        }

        @Test
        @DisplayName("participante repetido e recusado: o resultado seria ambiguo")
        void participanteRepetido() {
            assertThatThrownBy(() -> Rateio.distribuir(Dinheiro.deReais("10.00"),
                    List.of(Participacao.de("a", 1), Participacao.de("a", 2))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("repetido");
        }

        @Test
        @DisplayName("peso zero ou negativo e recusado")
        void pesoInvalido() {
            assertThatThrownBy(() -> Participacao.de("a", 0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Participacao.de("a", -1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("valor com mais de duas casas e recusado em vez de arredondado em silencio")
        void valorComTresCasas() {
            assertThatThrownBy(() -> Dinheiro.deReais("10.555"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duas casas");
        }

        @Test
        @DisplayName("total zero devolve zero para todos")
        void totalZero() {
            Map<String, Dinheiro> partes = Rateio.igualmente(Dinheiro.ZERO, List.of("a", "b", "c"));

            assertThat(partes.values()).allMatch(Dinheiro::ehZero);
        }
    }

    private static Dinheiro soma(Map<String, Dinheiro> partes) {
        return partes.values().stream().reduce(Dinheiro.ZERO, Dinheiro::somar);
    }
}
