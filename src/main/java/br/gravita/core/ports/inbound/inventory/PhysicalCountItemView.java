package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.PhysicalCount;
import java.util.Map;
import java.util.UUID;

/** A count with the code of every product on its lines, keyed by product id. */
public record PhysicalCountItemView(PhysicalCount count, Map<UUID, String> productCodes) {
}
