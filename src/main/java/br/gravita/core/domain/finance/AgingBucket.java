package br.gravita.core.domain.finance;

import java.math.BigDecimal;

/** One range of an {@link AgingReport}: how many titles fall in it and what is still owed on them. */
public record AgingBucket(AgingRange range, int titleCount, BigDecimal total) {
}
