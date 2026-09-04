package io.github.vmarins2005.rateio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

/**
 * Propriedades: o que precisa valer para <b>qualquer</b> entrada.
 *
 * <p>Os exemplos de {@code RateioTest} cobrem os casos que alguem imaginou. Estas
 * propriedades geram centenas de combinacoes por execucao e, quando encontram um
 * contraexemplo, reduzem ate o menor caso que ainda falha - que costuma ser o caso de
 * borda que ninguem teria escrito a mao.
 *
 * <p>As quatro primeiras sao as invariantes do dominio, e estao registradas no ADR 0001.
 */
class RateioPropriedadesTest {

    @Property
    void aSomaDasPartesEExatamenteOTotal(
            @ForAll @LongRange(min = 0, max = 1_000_000_000L) long centavos,
            @ForAll("participacoes") List<Participacao> participacoes) {

        Dinheiro total = Dinheiro.deCentavos(centavos);

        Map<String, Dinheiro> partes = Rateio.distribuir(total, participacoes);

        assertThat(soma(partes)).isEqualTo(total);
    }

    @Property
    void todaParteRecebeOPisoOuOPisoMaisUmCentavo(
            @ForAll @LongRange(min = 0, max = 1_000_000_000L) long centavos,
            @ForAll("participacoes") List<Participacao> participacoes) {

        Map<String, Dinheiro> partes = Rateio.distribuir(Dinheiro.deCentavos(centavos), participacoes);
        BigInteger somaDosPesos = participacoes.stream()
                .map(participacao -> BigInteger.valueOf(participacao.peso()))
                .reduce(BigInteger.ZERO, BigInteger::add);

        for (Participacao participacao : participacoes) {
            long piso = BigInteger.valueOf(centavos)
                    .multiply(BigInteger.valueOf(participacao.peso()))
                    .divide(somaDosPesos)
                    .longValueExact();
            long recebido = partes.get(participacao.id()).centavos();

            // Ninguem fica a mais de um centavo do valor ideal - nem para mais, nem para
            // menos. E o que impede o metodo de "fechar a conta" dando tudo a um so.
            assertThat(recebido).isBetween(piso, piso + 1);
        }
    }

    @Property
    void nenhumaParteENegativa(
            @ForAll @LongRange(min = 0, max = 1_000_000_000L) long centavos,
            @ForAll("participacoes") List<Participacao> participacoes) {

        Map<String, Dinheiro> partes = Rateio.distribuir(Dinheiro.deCentavos(centavos), participacoes);

        assertThat(partes.values()).allMatch(parte -> parte.centavos() >= 0);
    }

    @Property
    void embaralharAEntradaNaoMudaAParteDeNinguem(
            @ForAll @LongRange(min = 0, max = 1_000_000_000L) long centavos,
            @ForAll("participacoes") List<Participacao> participacoes,
            @ForAll @IntRange(min = 0, max = 10_000) int semente) {

        List<Participacao> embaralhada = new ArrayList<>(participacoes);
        Collections.shuffle(embaralhada, new Random(semente));

        Dinheiro total = Dinheiro.deCentavos(centavos);

        assertThat(Rateio.distribuir(total, embaralhada)).isEqualTo(Rateio.distribuir(total, participacoes));
    }

    @Property
    void comPesosIguaisAsPartesDiferemNoMaximoUmCentavo(
            @ForAll @LongRange(min = 0, max = 1_000_000_000L) long centavos,
            @ForAll @IntRange(min = 1, max = 20) int quantidade) {

        List<String> ids = IntStream.range(0, quantidade).mapToObj(indice -> "p" + indice).toList();

        Map<String, Dinheiro> partes = Rateio.igualmente(Dinheiro.deCentavos(centavos), ids);

        long menor = partes.values().stream().mapToLong(Dinheiro::centavos).min().orElseThrow();
        long maior = partes.values().stream().mapToLong(Dinheiro::centavos).max().orElseThrow();
        assertThat(maior - menor).isLessThanOrEqualTo(1);
    }

    @Property
    void ratearZeroDaZeroParaTodos(@ForAll("participacoes") List<Participacao> participacoes) {
        Map<String, Dinheiro> partes = Rateio.distribuir(Dinheiro.ZERO, participacoes);

        assertThat(partes.values()).allMatch(Dinheiro::ehZero);
    }

    @Property
    void multiplicarOPesoDeTodosNaoMudaOResultado(
            @ForAll @LongRange(min = 0, max = 1_000_000L) long centavos,
            @ForAll("participacoes") List<Participacao> participacoes,
            @ForAll @IntRange(min = 2, max = 100) int fator) {

        List<Participacao> ampliadas = participacoes.stream()
                .map(participacao -> Participacao.de(participacao.id(), participacao.peso() * fator))
                .toList();
        Dinheiro total = Dinheiro.deCentavos(centavos);

        // Peso e proporcao: dobrar todos os pesos nao muda proporcao nenhuma.
        assertThat(Rateio.distribuir(total, ampliadas)).isEqualTo(Rateio.distribuir(total, participacoes));
    }

    @Provide
    Arbitrary<List<Participacao>> participacoes() {
        return Arbitraries.integers().between(1, 12).flatMap(quantidade ->
                Arbitraries.longs().between(1, 10_000).list().ofSize(quantidade)
                        .map(pesos -> IntStream.range(0, quantidade)
                                .mapToObj(indice -> Participacao.de("p" + indice, pesos.get(indice)))
                                .toList()));
    }

    private static Dinheiro soma(Map<String, Dinheiro> partes) {
        return partes.values().stream().reduce(Dinheiro.ZERO, Dinheiro::somar);
    }
}
