package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.ProductDomain;
import java.util.UUID;

/** {@code productToRegister} carries every RegisterProductUseCase (UC-M1-11) field; its id is ignored and assigned fresh. */
public record ResolveInboundNfeItemAsNewProductCommand(UUID inboundNfeId, int itemIndex, ProductDomain productToRegister) {
}
