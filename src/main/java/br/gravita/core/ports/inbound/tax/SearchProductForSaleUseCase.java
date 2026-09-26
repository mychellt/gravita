package br.gravita.core.ports.inbound.tax;

import java.util.List;

public interface SearchProductForSaleUseCase {
	List<ProductSearchResult> execute(SearchProductQuery query);
}
