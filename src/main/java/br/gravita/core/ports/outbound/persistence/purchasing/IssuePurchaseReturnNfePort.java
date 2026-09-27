package br.gravita.core.ports.outbound.persistence.purchasing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface IssuePurchaseReturnNfePort {
	String issueReturnNfe(IssuePurchaseReturnNfeCommand command);

	record IssuePurchaseReturnNfeCommand(UUID purchaseReturnId, UUID supplierId, List<Item> items) {

		public IssuePurchaseReturnNfeCommand {
			Objects.requireNonNull(purchaseReturnId, "purchaseReturnId is required");
			Objects.requireNonNull(supplierId, "supplierId is required");
			items = items == null ? List.of() : List.copyOf(items);
		}

		public record Item(UUID productId, BigDecimal quantity, BigDecimal unitCost) {
			public Item {
				Objects.requireNonNull(productId, "productId is required");
				Objects.requireNonNull(quantity, "quantity is required");
				Objects.requireNonNull(unitCost, "unitCost is required");
			}
		}
	}
}
