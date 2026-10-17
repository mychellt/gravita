package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.ProductPurchase;
import br.gravita.core.ports.inbound.tax.GetProductPurchaseHistoryQuery;
import br.gravita.core.ports.inbound.tax.GetProductPurchaseHistoryUseCase;
import br.gravita.core.ports.outbound.persistence.tax.ProductPurchaseHistoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import java.util.List;

@UseCase
public class GetProductPurchaseHistoryService implements GetProductPurchaseHistoryUseCase {

	private final ProductPurchaseHistoryPort productPurchaseHistoryPort;
	private final CallerCompanyResolver callerCompanyResolver;

	public GetProductPurchaseHistoryService(final ProductPurchaseHistoryPort productPurchaseHistoryPort,
			final CallerCompanyResolver callerCompanyResolver) {
		this.productPurchaseHistoryPort = productPurchaseHistoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public List<ProductPurchase> execute(final GetProductPurchaseHistoryQuery query) {
		return callerCompanyResolver.resolve(new Context().withCaller(query.callerId()))
				.map(CompanyId::of)
				.map(companyId -> productPurchaseHistoryPort.findByProduct(companyId, query.productId()))
				.orElse(List.of());
	}
}
