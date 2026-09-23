package br.gravita.core.ports.outbound.persistence.purchasing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Purchasing-side extension point standing in for M2's {@code IssueNfeUseCase}
 * (`tax` context, GRA-8 - still backlog), which UC-M6-09 must invoke in the
 * same flow to issue the return NF-e (docs/specs/m6-compras/uc-09-return-to-supplier.md).
 * {@code IssueNfeUseCase} itself needs a full {@code NfeDocument}/tax-calculation
 * round trip that doesn't exist yet, so - mirroring {@link RegisterStockEntryPort}
 * and {@link GeneratePayableFromReceiptPort}'s stubs for M5/M8 - this narrower
 * port keeps {@code ReturnToSupplierService} runnable end-to-end until M2 ships
 * a real adapter that delegates to {@code IssueNfeUseCase} with
 * {@code naturezaOperacao} set to a return and {@code referencedAccessKey}
 * pointing at the original inbound NF-e.
 */
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
