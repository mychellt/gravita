package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.repositories.tax.ProductPurchaseHistoryJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.ProductPurchase;
import br.gravita.core.ports.outbound.persistence.tax.ProductPurchaseHistoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
class ProductPurchaseHistoryAdapter implements ProductPurchaseHistoryPort {

	private final ProductPurchaseHistoryJpaRepository jpaRepository;

	ProductPurchaseHistoryAdapter(final ProductPurchaseHistoryJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductPurchase> findByProduct(final CompanyId companyId, final UUID productId) {
		return jpaRepository.findPurchases(companyId.value(), productId).stream()
				.map(row -> new ProductPurchase(row.getInboundNfeId(), row.getSeries(), row.getNumber(), row.getSupplierDocument(),
						row.getSupplierName(), row.getIssuedAt(), row.getQuantity(), row.getUnitValue()))
				.toList();
	}
}
