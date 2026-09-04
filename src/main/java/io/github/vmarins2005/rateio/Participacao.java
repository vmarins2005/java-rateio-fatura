package io.github.vmarins2005.rateio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A parte de um participante, expressa em <b>peso</b> e nao em percentual.
 *
 * <p>Peso evita o problema mais chato de rateio por percentual: a soma dos percentuais
 * precisar dar exatamente 100. Com tres participantes iguais, nao existe percentual com
 * duas casas que some 100 - 33,33 tres vezes da 99,99, e 33,34 tres vezes da 100,02.
 *
 * <p>Com peso, cada um vale 1 e a soma dos pesos e o denominador. A pergunta some.
 *
 * <p>{@link #dePercentual(String, String)} continua disponivel para quem recebe percentual
 * de fora - ele so converte para peso, sem exigir que a soma de 100.
 */
public record Participacao(String id, long peso) {

    public Participacao {
        Objects.requireNonNull(id, "id do participante e obrigatorio");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id do participante nao pode ser vazio");
        }
        if (peso <= 0) {
            throw new IllegalArgumentException("peso deve ser positivo: " + peso);
        }
    }

    public static Participacao de(String id, long peso) {
        return new Participacao(id, peso);
    }

    /**
     * Converte percentual com ate duas casas em peso inteiro: {@code "33.33"} vira 3333.
     */
    public static Participacao dePercentual(String id, String percentual) {
        BigDecimal valor = new BigDecimal(percentual);
        if (valor.scale() > 2) {
            throw new IllegalArgumentException("percentual com mais de duas casas: " + percentual);
        }
        return new Participacao(id, valor.movePointRight(2).longValueExact());
    }
}
