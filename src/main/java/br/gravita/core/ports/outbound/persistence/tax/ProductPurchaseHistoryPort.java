package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.ProductPurchase;
import java.util.List;
import java.util.UUID;

public interface ProductPurchaseHistoryPort {

	/**
	 * The confirmed inbound NFe lines of {@code companyId} whose supplier product code is linked to {@code productId},
	 * newest first. Notes not yet confirmed are not purchases and never show up.
	 */
	List<ProductPurchase> findByProduct(CompanyId companyId, UUID productId);
}
