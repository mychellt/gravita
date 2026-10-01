package br.gravita.core.ports.inbound.reporting;

import java.util.List;

public interface GetSupplierPurchaseSummaryUseCase {
	List<SupplierPurchaseSummary> execute(SupplierPurchaseSummaryQuery query);
}
