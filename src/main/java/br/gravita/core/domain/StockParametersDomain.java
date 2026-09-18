package br.gravita.core.domain;

import java.math.BigDecimal;

public record StockParametersDomain(BigDecimal minimum, BigDecimal maximum, BigDecimal reorderPoint) {
}
