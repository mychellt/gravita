package br.gravita.inventory.application.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;
import br.gravita.core.ports.inbound.inventory.SuggestReorderQuery;
import br.gravita.core.ports.inbound.inventory.SuggestReorderUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.CreatePurchaseRequestPort;
import br.gravita.core.usercases.inventory.LowStockReorderTrigger;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GRA-88: unit coverage for the M5-11 automatic-trigger orchestrator between
 * {@code SuggestReorderUseCase} and M6's {@code CreatePurchaseRequestUseCase}
 * (bridged through {@link CreatePurchaseRequestPort}).
 */
@ExtendWith(MockitoExtension.class)
class LowStockReorderTriggerTest {

	@Mock
	private SuggestReorderUseCase suggestReorderUseCase;

	@Mock
	private CreatePurchaseRequestPort createPurchaseRequestPort;

	private final UUID warehouseId = UUID.randomUUID();

	private LowStockReorderTrigger trigger;

	@Test
	void ac1_createsAPurchaseRequestForEachSuggestionProducedForTheWarehouse() {
		trigger = new LowStockReorderTrigger(suggestReorderUseCase, createPurchaseRequestPort);
		UUID productId = UUID.randomUUID();
		ReorderSuggestion suggestion = new ReorderSuggestion(productId, warehouseId, new BigDecimal("15"),
				new BigDecimal("20"), new BigDecimal("85"));
		when(suggestReorderUseCase.execute(new SuggestReorderQuery(warehouseId))).thenReturn(List.of(suggestion));

		trigger.evaluate(warehouseId);

		ArgumentCaptor<CreatePurchaseRequestPort.ReorderCommand> captor = ArgumentCaptor
				.forClass(CreatePurchaseRequestPort.ReorderCommand.class);
		verify(createPurchaseRequestPort).createIfNotAlreadyOpen(captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().productId()).isEqualTo(productId);
		org.assertj.core.api.Assertions.assertThat(captor.getValue().quantity()).isEqualByComparingTo("85");
	}

	@Test
	void doesNothingWhenNoSuggestionsAreProduced() {
		trigger = new LowStockReorderTrigger(suggestReorderUseCase, createPurchaseRequestPort);
		when(suggestReorderUseCase.execute(new SuggestReorderQuery(warehouseId))).thenReturn(List.of());

		trigger.evaluate(warehouseId);

		verifyNoInteractions(createPurchaseRequestPort);
	}

	@Test
	void skipsASuggestionWithNoPositiveReplenishmentQuantity() {
		trigger = new LowStockReorderTrigger(suggestReorderUseCase, createPurchaseRequestPort);
		ReorderSuggestion suggestion = new ReorderSuggestion(UUID.randomUUID(), warehouseId, new BigDecimal("15"),
				new BigDecimal("20"), BigDecimal.ZERO);
		when(suggestReorderUseCase.execute(new SuggestReorderQuery(warehouseId))).thenReturn(List.of(suggestion));

		trigger.evaluate(warehouseId);

		verifyNoInteractions(createPurchaseRequestPort);
	}
}
