package br.gravita.core.usercases.purchasing;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.ports.inbound.purchasing.ListPurchaseOrdersUseCase;
import br.gravita.core.ports.inbound.purchasing.PurchaseOrderListItem;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@UseCase
public class ListPurchaseOrdersService implements ListPurchaseOrdersUseCase {

	private final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
	private final SupplierRepositoryPort supplierRepositoryPort;

	public ListPurchaseOrdersService(final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort,
			final SupplierRepositoryPort supplierRepositoryPort) {
		this.purchaseOrderRepositoryPort = purchaseOrderRepositoryPort;
		this.supplierRepositoryPort = supplierRepositoryPort;
	}

	@Override
	public List<PurchaseOrderListItem> execute() {
		final Map<UUID, Optional<String>> names = new HashMap<>();
		return purchaseOrderRepositoryPort.findAllNewestFirst().stream().map(record -> {
			final var supplierId = record.order().getSupplierId();
			final Optional<String> name = names.computeIfAbsent(supplierId.value(),
					id -> supplierRepositoryPort.findById(supplierId).map(Supplier::getName));
			return new PurchaseOrderListItem(record.order(), record.createdAt(), name.orElse(null));
		}).toList();
	}
}
