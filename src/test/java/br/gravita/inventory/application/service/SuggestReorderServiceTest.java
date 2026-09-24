package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;
import br.gravita.core.ports.inbound.inventory.SuggestReorderQuery;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.usercases.inventory.SuggestReorderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuggestReorderServiceTest {

	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private SuggestReorderService service;

	private final UUID productId = UUID.randomUUID();
	private final UUID warehouseId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new SuggestReorderService(stockBalanceRepositoryPort, productRepositoryPort);
	}

	@Test
	void suggestsReorderWhenAvailableIsBelowTheReorderPoint() {
		StockBalance balance = balanceOf("15");
		when(stockBalanceRepositoryPort.findAll()).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWithStock("10", "100", "20")));

		List<ReorderSuggestion> suggestions = service.execute(SuggestReorderQuery.fullSweep());

		assertThat(suggestions).hasSize(1);
		ReorderSuggestion suggestion = suggestions.get(0);
		assertThat(suggestion.productId()).isEqualTo(productId);
		assertThat(suggestion.warehouseId()).isEqualTo(warehouseId);
		assertThat(suggestion.available()).isEqualByComparingTo("15");
		assertThat(suggestion.reorderPoint()).isEqualByComparingTo("20");
	}

	@Test
	void suggestsReorderAtTheExactBoundaryWhereAvailableEqualsTheReorderPoint() {
		StockBalance balance = balanceOf("20");
		when(stockBalanceRepositoryPort.findAll()).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWithStock("10", "100", "20")));

		List<ReorderSuggestion> suggestions = service.execute(SuggestReorderQuery.fullSweep());

		assertThat(suggestions).hasSize(1);
	}

	@Test
	void producesNoSuggestionWhenAvailableIsAboveTheReorderPoint() {
		StockBalance balance = balanceOf("21");
		when(stockBalanceRepositoryPort.findAll()).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWithStock("10", "100", "20")));

		List<ReorderSuggestion> suggestions = service.execute(SuggestReorderQuery.fullSweep());

		assertThat(suggestions).isEmpty();
	}

	@Test
	void suggestedQuantityBringsTheBalanceBackToTheConfiguredMaximumNotJustToTheReorderPoint() {
		StockBalance balance = balanceOf("15");
		when(stockBalanceRepositoryPort.findAll()).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWithStock("10", "100", "20")));

		List<ReorderSuggestion> suggestions = service.execute(SuggestReorderQuery.fullSweep());

		// maximum (100) - available (15), not reorderPoint (20) - available (15)
		assertThat(suggestions.get(0).suggestedQuantity()).isEqualByComparingTo("85");
	}

	@Test
	void skipsAProductWithNoConfiguredReorderPoint() {
		StockBalance balance = balanceOf("0");
		when(stockBalanceRepositoryPort.findAll()).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId))
				.thenReturn(Optional.of(productWithStock(null, null, null)));

		List<ReorderSuggestion> suggestions = service.execute(SuggestReorderQuery.fullSweep());

		assertThat(suggestions).isEmpty();
	}

	@Test
	void filtersByWarehouseWhenAWarehouseIdIsProvided() {
		StockBalance balance = balanceOf("15");
		when(stockBalanceRepositoryPort.findByWarehouseId(warehouseId)).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWithStock("10", "100", "20")));

		List<ReorderSuggestion> suggestions = service.execute(new SuggestReorderQuery(warehouseId));

		assertThat(suggestions).hasSize(1);
	}

	private StockBalance balanceOf(String onHand) {
		return StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId, new BigDecimal(onHand),
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
	}

	private ProductDomain productWithStock(String minimum, String maximum, String reorderPoint) {
		return ProductDomain.builder()
				.id(productId)
				.stock(new StockParametersDomain(bigDecimalOrNull(minimum), bigDecimalOrNull(maximum),
						bigDecimalOrNull(reorderPoint)))
				.build();
	}

	private BigDecimal bigDecimalOrNull(String value) {
		return value == null ? null : new BigDecimal(value);
	}
}
