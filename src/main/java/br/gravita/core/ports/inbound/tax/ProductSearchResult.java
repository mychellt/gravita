package br.gravita.core.ports.inbound.tax;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSearchResult(UUID productId, String description, BigDecimal unitPrice, boolean availableForSale) {
}
