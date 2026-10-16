package br.gravita.core.usercases.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;

/** Shared by the two "resolve an unresolved item" use cases (GRA-246 BR4/BR5): supplier lookup, item bounds, and the one-link-per-code guard. */
final class InboundNfeItemResolutionSupport {

	private InboundNfeItemResolutionSupport() {
	}

	static Supplier requireResolvedSupplier(final SupplierRepositoryPort supplierRepositoryPort, final InboundNfe inboundNfe) {
		return supplierRepositoryPort.findByDocument(inboundNfe.getSupplierDocument())
				.orElseThrow(() -> new BusinessRuleException(
						"No supplier is registered for CNPJ " + inboundNfe.getSupplierDocument().formatted()
								+ "; register the supplier before resolving its items"));
	}

	static InboundNfeItem requireItemAt(final InboundNfe inboundNfe, final int itemIndex) {
		if (itemIndex < 0 || itemIndex >= inboundNfe.getItems().size()) {
			throw new BusinessRuleException("Item index out of range for this NFe: " + itemIndex);
		}
		return inboundNfe.getItems().get(itemIndex);
	}

	static void requireNotAlreadyLinked(final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort, final CompanyId companyId,
			final SupplierId supplierId, final InboundNfeItem item) {
		if (linkRepositoryPort.findByCompanySupplierAndCode(companyId, supplierId, item.supplierProductCode()).isPresent()) {
			throw new BusinessRuleException(
					"Item " + item.supplierProductCode() + " is already linked to a catalog product for this supplier");
		}
	}
}
