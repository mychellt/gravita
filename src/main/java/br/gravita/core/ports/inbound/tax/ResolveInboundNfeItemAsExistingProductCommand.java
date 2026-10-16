package br.gravita.core.ports.inbound.tax;

import java.util.UUID;

public record ResolveInboundNfeItemAsExistingProductCommand(UUID inboundNfeId, int itemIndex, UUID productId) {
}
