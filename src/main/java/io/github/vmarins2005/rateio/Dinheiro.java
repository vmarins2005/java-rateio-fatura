package io.github.vmarins2005.rateio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Dinheiro representado em <b>centavos inteiros</b>, e nao em {@code BigDecimal}.
 *
 * <p>Para rateio, essa escolha e a diferenca entre exato e quase exato. A conta central do
 * problema - "dividir 10000 centavos em tres partes e distribuir o resto" - e aritmetica
 * inteira: divisao com resto, sem arredondamento e sem casa decimal escondida.
 *
 * <p>Com {@code BigDecimal} a mesma conta exige escolher escala e modo de arredondamento
 * em cada passo, e a soma das partes passa a depender dessas escolhas. Aqui a soma das
 * partes e igual ao total por construcao, e nao por sorte. Ver ADR 0001.
 *
 * <p>O limite: centavo e a menor unidade. Domínios que precisam de fracao de centavo -
 * juros diarios, cambio, combustivel - precisam de outro tipo.
 */
public record Dinheiro(long centavos) {

    public static final Dinheiro ZERO = new Dinheiro(0);

    public Dinheiro {
        if (centavos < 0) {
            throw new IllegalArgumentException("valor monetario nao pode ser negativo: " + centavos);
        }
    }

    public static Dinheiro deCentavos(long centavos) {
        return new Dinheiro(centavos);
    }

    /**
     * Aceita {@code "10"}, {@code "10.5"} e {@code "10.50"}. Mais de duas casas e recusado
     * em vez de arredondado em silencio: quem escreveu {@code "10.555"} tem um defeito no
     * codigo, e arredondar esconderia isso.
     */
    public static Dinheiro deReais(String reais) {
        Objects.requireNonNull(reais, "valor e obrigatorio");
        BigDecimal valor = new BigDecimal(reais);
        if (valor.scale() > 2) {
            throw new IllegalArgumentException("valor com mais de duas casas decimais: " + reais);
        }
        return new Dinheiro(valor.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact());
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(Math.addExact(centavos, outro.centavos));
    }

    public boolean ehZero() {
        return centavos == 0;
    }

    public BigDecimal emReais() {
        return BigDecimal.valueOf(centavos, 2);
    }

    @Override
    public String toString() {
        return "R$ " + emReais().toPlainString();
    }
}
