package io.github.vmarins2005.rateio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Estes testes nasceram do relatorio do PITest.
 *
 * <p>{@code emReais} e {@code toString} apareciam como mutacoes sem cobertura: nenhum
 * teste tocava neles. Sao a superficie de apresentacao do tipo - o que aparece em nota,
 * em tela e em log - e um erro ali chega ao usuario final, entao ganharam teste em vez de
 * serem removidos.
 *
 * <p>{@code subtrair} e {@code compareTo} apareciam pelo mesmo motivo, e tiveram destino
 * oposto: nao eram usados por ninguem, e foram apagados. Ver ADR 0003.
 */
class DinheiroTest {

    @Test
    @DisplayName("converte reais para centavos, com uma ou duas casas")
    void converteDeReais() {
        assertThat(Dinheiro.deReais("10").centavos()).isEqualTo(1000);
        assertThat(Dinheiro.deReais("10.5").centavos()).isEqualTo(1050);
        assertThat(Dinheiro.deReais("10.55").centavos()).isEqualTo(1055);
        assertThat(Dinheiro.deReais("0.01").centavos()).isEqualTo(1);
    }

    @Test
    @DisplayName("recusa mais de duas casas em vez de arredondar em silencio")
    void recusaTresCasas() {
        assertThatThrownBy(() -> Dinheiro.deReais("10.555"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("nao existe dinheiro negativo")
    void recusaNegativo() {
        assertThatThrownBy(() -> Dinheiro.deCentavos(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("volta para reais com duas casas, inclusive quando o valor e redondo")
    void voltaParaReais() {
        assertThat(Dinheiro.deCentavos(1055).emReais()).isEqualByComparingTo("10.55");
        assertThat(Dinheiro.deCentavos(1000).emReais().toPlainString()).isEqualTo("10.00");
        assertThat(Dinheiro.deCentavos(1).emReais().toPlainString()).isEqualTo("0.01");
        assertThat(Dinheiro.ZERO.emReais().toPlainString()).isEqualTo("0.00");
    }

    @Test
    @DisplayName("a representacao em texto e a que vai para log e para tela")
    void representacaoEmTexto() {
        assertThat(Dinheiro.deCentavos(1055)).hasToString("R$ 10.55");
        assertThat(Dinheiro.ZERO).hasToString("R$ 0.00");
    }

    @Test
    @DisplayName("somar acumula centavos")
    void somar() {
        assertThat(Dinheiro.deReais("10.55").somar(Dinheiro.deReais("0.45")))
                .isEqualTo(Dinheiro.deReais("11.00"));
    }
}
