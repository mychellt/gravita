package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.ports.inbound.inventory.InventorySummary;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.usercases.inventory.GetInventorySummaryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetInventorySummaryServiceTest {

	@Mock
	private ProductRepositoryPort productRepositoryPort;
	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	private ProductDomain product(final UUID id, final ProductStatus status, final String minimum) {
		return ProductDomain.builder().id(id).status(status)
				.stock(minimum == null ? null : new StockParametersDomain(new BigDecimal(minimum), null, null)).build();
	}

	private StockBalance balance(final UUID productId, final String available) {
		final StockBalance balance = mock(StockBalance.class);
		when(balance.getProductId()).thenReturn(productId);
		when(balance.available()).thenReturn(new BigDecimal(available));
		return balance;
	}

	@Test
	@DisplayName("Counts the active products and those whose stock over all warehouses is under the minimum")
	void countsActiveSkusAndBelowMinimum() {
		final UUID low = UUID.randomUUID();
		final UUID splitOk = UUID.randomUUID();
		final UUID noBalance = UUID.randomUUID();
		final UUID noMinimum = UUID.randomUUID();
		final UUID inactive = UUID.randomUUID();
		when(productRepositoryPort.findAll()).thenReturn(List.of(
				product(low, ProductStatus.ACTIVE, "10"), product(splitOk, ProductStatus.ACTIVE, "10"),
				product(noBalance, ProductStatus.ACTIVE, "5"), product(noMinimum, ProductStatus.ACTIVE, null),
				product(inactive, ProductStatus.INACTIVE, "10")));
		final List<StockBalance> balances = List.of(balance(low, "4"), balance(splitOk, "6"), balance(splitOk, "5"), balance(inactive, "0"));
		when(stockBalanceRepositoryPort.findAll()).thenReturn(balances);

		final InventorySummary summary = new GetInventorySummaryService(productRepositoryPort, stockBalanceRepositoryPort).execute();

		assertThat(summary.activeSkus()).isEqualTo(4);
		assertThat(summary.belowMinimum()).isEqualTo(2);
	}
}
