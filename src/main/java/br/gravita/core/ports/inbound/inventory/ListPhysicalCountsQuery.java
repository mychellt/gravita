package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.PhysicalCountStatus;
import br.gravita.core.domain.shared.PageQuery;

public record ListPhysicalCountsQuery(PhysicalCountStatus status, PageQuery pageQuery) {
}
