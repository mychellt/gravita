package br.gravita.adapters.outbound.persistence.adapters.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestCommand;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.CreatePurchaseRequestPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GRA-88: unit coverage for the M5->M6 reorder bridge, in particular AC2
 * (no duplicate OPEN MIN_STOCK_TRIGGER request for the same product).
 */
@ExtendWith(MockitoExtension.class)
class CreatePurchaseRequestAdapterTest {

	@Mock
	private CreatePurchaseRequestUseCase createPurchaseRequestUseCase;

	@Mock
	private PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;

	private CreatePurchaseRequestAdapter adapter;

	private final UUID productId = UUID.randomUUID();

	@Test
	void ac1_createsAMinStockTriggerRequestWhenNoneIsAlreadyOpenForTheProduct() {
		adapter = new CreatePurchaseRequestAdapter(createPurchaseRequestUseCase, purchaseRequestRepositoryPort);
		when(purchaseRequestRepositoryPort.existsOpenByOriginAndProductId(PurchaseRequestOrigin.MIN_STOCK_TRIGGER,
				productId)).thenReturn(false);

		adapter.createIfNotAlreadyOpen(new CreatePurchaseRequestPort.ReorderCommand(productId, new BigDecimal("85")));

		ArgumentCaptor<CreatePurchaseRequestCommand> captor = ArgumentCaptor.forClass(CreatePurchaseRequestCommand.class);
		verify(createPurchaseRequestUseCase).execute(captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().origin())
				.isEqualTo(PurchaseRequestOrigin.MIN_STOCK_TRIGGER);
		org.assertj.core.api.Assertions.assertThat(captor.getValue().requestedBy()).isNull();
		org.assertj.core.api.Assertions.assertThat(captor.getValue().items()).hasSize(1);
		org.assertj.core.api.Assertions.assertThat(captor.getValue().items().get(0).productId()).isEqualTo(productId);
		org.assertj.core.api.Assertions.assertThat(captor.getValue().items().get(0).quantity())
				.isEqualByComparingTo("85");
	}

	@Test
	void ac2_skipsCreationWhenAnOpenMinStockTriggerRequestAlreadyExistsForTheProduct() {
		adapter = new CreatePurchaseRequestAdapter(createPurchaseRequestUseCase, purchaseRequestRepositoryPort);
		when(purchaseRequestRepositoryPort.existsOpenByOriginAndProductId(PurchaseRequestOrigin.MIN_STOCK_TRIGGER,
				productId)).thenReturn(true);

		adapter.createIfNotAlreadyOpen(new CreatePurchaseRequestPort.ReorderCommand(productId, new BigDecimal("85")));

		verify(createPurchaseRequestUseCase, never()).execute(any());
	}
}
