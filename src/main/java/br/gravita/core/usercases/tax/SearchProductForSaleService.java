package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerPriceTableLink;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.ProductOrClassRef;
import br.gravita.core.ports.inbound.tax.ProductSearchResult;
import br.gravita.core.ports.inbound.tax.SearchProductForSaleUseCase;
import br.gravita.core.ports.inbound.tax.SearchProductQuery;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@UseCase
public class SearchProductForSaleService implements SearchProductForSaleUseCase {

	private final ProductRepositoryPort productRepositoryPort;
	private final PriceTableRepositoryPort priceTableRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;

	public SearchProductForSaleService(ProductRepositoryPort productRepositoryPort,
			PriceTableRepositoryPort priceTableRepositoryPort, CustomerRepositoryPort customerRepositoryPort) {
		this.productRepositoryPort = productRepositoryPort;
		this.priceTableRepositoryPort = priceTableRepositoryPort;
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	public List<ProductSearchResult> execute(SearchProductQuery query) {
		List<CustomerPriceTableLink> customerPriceTables = resolveCustomerPriceTables(query.customerId());

		return productRepositoryPort.findAll().stream()
				.filter(product -> product.getStatus() == ProductStatus.ACTIVE)
				.filter(product -> matches(product, query.searchTerm()))
				.map(product -> toResult(product, customerPriceTables))
				.toList();
	}

	private List<CustomerPriceTableLink> resolveCustomerPriceTables(java.util.UUID customerId) {
		if (customerId == null) {
			return List.of();
		}
		return customerRepositoryPort.get(customerId)
				.map(CustomerDomain::getPriceTables)
				.orElse(List.of())
				.stream()
				.sorted(Comparator.comparing(CustomerPriceTableLink::getPriority))
				.toList();
	}

	private boolean matches(ProductDomain product, String searchTerm) {
		String normalizedTerm = searchTerm.trim().toLowerCase();
		if (product.getBarcodes() != null
				&& product.getBarcodes().stream().anyMatch(barcode -> barcode.equalsIgnoreCase(normalizedTerm))) {
			return true;
		}
		return product.getInternalCode() != null && product.getInternalCode().toLowerCase().contains(normalizedTerm);
	}

	private ProductSearchResult toResult(ProductDomain product, List<CustomerPriceTableLink> customerPriceTables) {
		BigDecimal price = resolvePrice(product, customerPriceTables);
		return new ProductSearchResult(product.getId(), product.getInternalCode(), price,
				product.getStatus() == ProductStatus.ACTIVE);
	}

	private BigDecimal resolvePrice(ProductDomain product, List<CustomerPriceTableLink> customerPriceTables) {
		ProductOrClassRef ref = ProductOrClassRef.product(product.getId().toString());
		LocalDate today = LocalDate.now();

		for (CustomerPriceTableLink link : customerPriceTables) {
			Optional<PriceTable> table = priceTableRepositoryPort.findById(PriceTableId.of(link.getPriceTableId()));
			if (table.isEmpty() || !table.get().isActive(today) || !hasEntryFor(table.get(), ref)) {
				continue;
			}
			return table.get().resolvePrice(ref, product.getAverageCost(), product.getBasePrice());
		}
		return product.getBasePrice();
	}

	private boolean hasEntryFor(PriceTable table, ProductOrClassRef ref) {
		return table.getEntries().stream().anyMatch(entry -> Objects.equals(entry.ref(), ref));
	}
}
