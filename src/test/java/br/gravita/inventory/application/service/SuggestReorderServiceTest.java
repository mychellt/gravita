package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;
import br.gravita.core.ports.inbound.inventory.SuggestReorderQuery;
import br.gravita.core.ports.outbound.inventory.NotifyLowStockPort;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.usercases.inventory.SuggestReorderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuggestReorderServiceTest {

	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	@Mock
	private NotifyLowStockPort notifyLowStockPort;

	private SuggestReorderService service;

	private final UUID productId = UUID.randomUUID();
	private final UUID warehouseId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new SuggestReorderService(stockBalanceRepositoryPort, productRepositoryPort, notifyLowStockPort);
	}

	@Test
	@DisplayName("Suggests a reorder when available stock is below the reorder point")
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
	@DisplayName("Suggests a reorder when available stock equals the reorder point exactly")
	void suggestsReorderAtTheExactBoundaryWhereAvailableEqualsTheReorderPoint() {
		StockBalance balance = balanceOf("20");
		when(stockBalanceRepositoryPort.findAll()).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWithStock("10", "100", "20")));

		List<ReorderSuggestion> suggestions = service.execute(SuggestReorderQuery.fullSweep());

		assertThat(suggestions).hasSize(1);
	}

	@Test
	@DisplayName("Produces no suggestion when available stock is above the reorder point")
	void producesNoSuggestionWhenAvailableIsAboveTheReorderPoint() {
		StockBalance balance = balanceOf("21");
		when(stockBalanceRepositoryPort.findAll()).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWithStock("10", "100", "20")));

		List<ReorderSuggestion> suggestions = service.execute(SuggestReorderQuery.fullSweep());

		assertThat(suggestions).isEmpty();
	}

	@Test
	@DisplayName("The suggested quantity brings the balance back up to the configured maximum, not just the reorder point")
	void suggestedQuantityBringsTheBalanceBackToTheConfiguredMaximumNotJustToTheReorderPoint() {
		StockBalance balance = balanceOf("15");
		when(stockBalanceRepositoryPort.findAll()).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWithStock("10", "100", "20")));

		List<ReorderSuggestion> suggestions = service.execute(SuggestReorderQuery.fullSweep());

		assertThat(suggestions.get(0).suggestedQuantity()).isEqualByComparingTo("85");
	}

	@Test
	@DisplayName("Skips a product that has no reorder point configured")
	void skipsAProductWithNoConfiguredReorderPoint() {
		StockBalance balance = balanceOf("0");
		when(stockBalanceRepositoryPort.findAll()).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId))
				.thenReturn(Optional.of(productWithStock(null, null, null)));

		List<ReorderSuggestion> suggestions = service.execute(SuggestReorderQuery.fullSweep());

		assertThat(suggestions).isEmpty();
	}

	@Test
	@DisplayName("Considers only the given warehouse when a warehouse id is provided")
	void filtersByWarehouseWhenAWarehouseIdIsProvided() {
		StockBalance balance = balanceOf("15");
		when(stockBalanceRepositoryPort.findByWarehouseId(warehouseId)).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWithStock("10", "100", "20")));

		List<ReorderSuggestion> suggestions = service.execute(new SuggestReorderQuery(warehouseId));

		assertThat(suggestions).hasSize(1);
	}

	@Test
	@DisplayName("AC4: Notifies the low-stock port with the produced suggestions")
	void ac4NotifiesLowStockPortWithTheProducedSuggestions() {
		StockBalance balance = balanceOf("15");
		when(stockBalanceRepositoryPort.findAll()).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWithStock("10", "100", "20")));

		List<ReorderSuggestion> suggestions = service.execute(SuggestReorderQuery.fullSweep());

		ArgumentCaptor<List<ReorderSuggestion>> captor = ArgumentCaptor.forClass(List.class);
		verify(notifyLowStockPort).notify(captor.capture());
		assertThat(captor.getValue()).isEqualTo(suggestions);
	}

	@Test
	@DisplayName("AC4: Notifies the low-stock port with an empty list when no suggestion is produced")
	void ac4NotifiesLowStockPortWithAnEmptyListWhenNoSuggestionIsProduced() {
		StockBalance balance = balanceOf("21");
		when(stockBalanceRepositoryPort.findAll()).thenReturn(List.of(balance));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWithStock("10", "100", "20")));

		service.execute(SuggestReorderQuery.fullSweep());

		verify(notifyLowStockPort).notify(List.of());
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
