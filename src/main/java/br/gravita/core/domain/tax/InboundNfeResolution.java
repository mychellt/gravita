package br.gravita.core.domain.tax;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import java.util.List;
import java.util.Optional;

/**
 * How far an {@link InboundNfe} is resolved against the registries: whether its supplier CNPJ matches a registered
 * Supplier and, per item, whether a catalog link exists for (supplier, supplier product code). Derived on demand,
 * never persisted, and it never touches the imported item snapshot.
 */
public record InboundNfeResolution(Optional<SupplierId> supplierId, List<ItemResolution> items) {

	public InboundNfeResolution {
		items = List.copyOf(items);
	}

	public boolean isSupplierResolved() {
		return supplierId.isPresent();
	}

	public List<ItemResolution> unresolvedItems() {
		return items.stream().filter(item -> !item.isResolved()).toList();
	}

	/** An item of the NFe, by position, and the catalog link its supplier code resolved to, if any. */
	public record ItemResolution(int index, InboundNfeItem item, Optional<SupplierProductCatalogLink> link) {

		public boolean isResolved() {
			return link.isPresent();
		}
	}
}
