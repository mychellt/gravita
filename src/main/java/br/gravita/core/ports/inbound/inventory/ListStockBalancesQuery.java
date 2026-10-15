package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.shared.PageQuery;
import java.util.UUID;

public record ListStockBalancesQuery(UUID productId, PageQuery pageQuery) {
}
