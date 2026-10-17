package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.UserId;
import java.util.Objects;
import java.util.UUID;

/**
 * Resolves one item of an inbound NFe either by linking it to an existing catalog Product or by registering a new
 * Product (already completed by the operator) and linking that. Exactly one of the two is set.
 */
public record ResolveInboundNfeItemCommand(UserId callerId, UUID inboundNfeId, int itemIndex, UUID existingProductId,
		ProductDomain newProduct) {

	public ResolveInboundNfeItemCommand {
		Objects.requireNonNull(callerId, "callerId is required");
		Objects.requireNonNull(inboundNfeId, "inboundNfeId is required");
		if ((existingProductId == null) == (newProduct == null)) {
			throw new BusinessRuleException("Inform either an existing product or a new product to resolve the item");
		}
	}

	public static ResolveInboundNfeItemCommand linkingExisting(final UserId callerId, final UUID inboundNfeId,
			final int itemIndex, final UUID productId) {
		return new ResolveInboundNfeItemCommand(callerId, inboundNfeId, itemIndex, productId, null);
	}

	public static ResolveInboundNfeItemCommand registeringNew(final UserId callerId, final UUID inboundNfeId,
			final int itemIndex, final ProductDomain product) {
		return new ResolveInboundNfeItemCommand(callerId, inboundNfeId, itemIndex, null, product);
	}
}
