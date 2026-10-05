package br.gravita.core.usercases.purchasing;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReturn;
import br.gravita.core.domain.purchasing.PurchaseReturnId;
import br.gravita.core.domain.purchasing.PurchaseReturnItem;
import br.gravita.core.ports.inbound.purchasing.ReturnToSupplierCommand;
import br.gravita.core.ports.inbound.purchasing.ReturnToSupplierUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.IssuePurchaseReturnNfePort;
import br.gravita.core.ports.outbound.persistence.purchasing.IssuePurchaseReturnNfePort.IssuePurchaseReturnNfeCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReturnRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.ReversePayableFromReturnPort;
import br.gravita.core.ports.outbound.persistence.purchasing.ReversePayableFromReturnPort.ReversePayableFromReturnCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.ReverseStockEntryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.ReverseStockEntryPort.ReverseStockEntryCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@UseCase
public class ReturnToSupplierService implements ReturnToSupplierUseCase {

	private final PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;
	private final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
	private final PurchaseReturnRepositoryPort purchaseReturnRepositoryPort;
	private final ReverseStockEntryPort reverseStockEntryPort;
	private final ReversePayableFromReturnPort reversePayableFromReturnPort;
	private final IssuePurchaseReturnNfePort issuePurchaseReturnNfePort;

	public ReturnToSupplierService(final PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort,
			final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort,
			final PurchaseReturnRepositoryPort purchaseReturnRepositoryPort, final ReverseStockEntryPort reverseStockEntryPort,
			final ReversePayableFromReturnPort reversePayableFromReturnPort,
			final IssuePurchaseReturnNfePort issuePurchaseReturnNfePort) {
		this.purchaseReceiptRepositoryPort = purchaseReceiptRepositoryPort;
		this.purchaseOrderRepositoryPort = purchaseOrderRepositoryPort;
		this.purchaseReturnRepositoryPort = purchaseReturnRepositoryPort;
		this.reverseStockEntryPort = reverseStockEntryPort;
		this.reversePayableFromReturnPort = reversePayableFromReturnPort;
		this.issuePurchaseReturnNfePort = issuePurchaseReturnNfePort;
	}

	@Override
	public PurchaseReturnId execute(final ReturnToSupplierCommand command) {
		final PurchaseReceipt receipt = purchaseReceiptRepositoryPort.findById(command.receiptId())
				.orElseThrow(() -> new PurchaseReceiptNotFoundException(command.receiptId().value()));
		final PurchaseOrder order = purchaseOrderRepositoryPort.findById(receipt.getOrderId())
				.orElseThrow(() -> new PurchaseOrderNotFoundException(receipt.getOrderId().value()));

		final List<PurchaseReturnItem> items = command.items().stream()
				.map(item -> new PurchaseReturnItem(item.productId(), item.quantity()))
				.toList();
		final List<PurchaseReturn> previousReturns = purchaseReturnRepositoryPort.findByReceiptId(receipt.getId());

		final PurchaseReturn purchaseReturn = PurchaseReturn.forReceipt(PurchaseReturnId.of(UUID.randomUUID()), receipt,
				items, previousReturns);

		for (final PurchaseReturnItem item : purchaseReturn.getItems()) {
			final BigDecimal unitCost = resolveUnitCost(order, item.productId());
			reverseStockEntryPort.reverseEntry(new ReverseStockEntryCommand(item.productId(), item.quantity(),
					unitCost, purchaseReturn.getId().value()));
		}

		final BigDecimal returnedAmount = purchaseReturn.getItems().stream()
				.map(item -> resolveUnitCost(order, item.productId()).multiply(item.quantity()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		reversePayableFromReturnPort.reversePayable(new ReversePayableFromReturnCommand(purchaseReturn.getId().value(),
				order.getSupplierId().value(), returnedAmount));

		final String returnNfeRef = issuePurchaseReturnNfePort
				.issueReturnNfe(new IssuePurchaseReturnNfeCommand(purchaseReturn.getId().value(),
						order.getSupplierId().value(), toNfeItems(order, purchaseReturn)));

		final PurchaseReturn saved = purchaseReturnRepositoryPort.save(purchaseReturn.withNfeRef(returnNfeRef));
		return saved.getId();
	}

	private BigDecimal resolveUnitCost(final PurchaseOrder order, final UUID productId) {
		return order.getItems().stream()
				.filter(item -> item.productId().equals(productId))
				.map(PurchaseOrderItem::unitPrice)
				.findFirst()
				.orElse(BigDecimal.ZERO);
	}

	private List<IssuePurchaseReturnNfeCommand.Item> toNfeItems(final PurchaseOrder order, final PurchaseReturn purchaseReturn) {
		return purchaseReturn.getItems().stream()
				.map(item -> new IssuePurchaseReturnNfeCommand.Item(item.productId(), item.quantity(),
						resolveUnitCost(order, item.productId())))
				.toList();
	}
}
