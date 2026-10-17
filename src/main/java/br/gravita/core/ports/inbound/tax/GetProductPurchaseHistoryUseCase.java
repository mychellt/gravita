package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.ProductPurchase;
import java.util.List;

/** What the caller's company paid for a product, supplier by supplier, from its confirmed inbound NFe; newest first. */
public interface GetProductPurchaseHistoryUseCase {
	List<ProductPurchase> execute(GetProductPurchaseHistoryQuery query);
}
