package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseRequestPersistenceMapperImpl;
import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestCommand;
import br.gravita.core.usercases.purchasing.CreatePurchaseRequestService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({PurchaseRequestRepositoryAdapter.class, PurchaseRequestPersistenceMapperImpl.class})
class CreatePurchaseRequestIntegrationTest {

	@Autowired
	private PurchaseRequestRepositoryAdapter purchaseRequestRepositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	private CreatePurchaseRequestService service;

	@BeforeEach
	void setUp() {
		service = new CreatePurchaseRequestService(purchaseRequestRepositoryAdapter);
	}

	@Test
	@DisplayName("Persists a manual request with its items and starts it open")
	void creatingAManualRequestPersistsItsItemsAndStartsOpen() {
		UUID requestedBy = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		var id = service.execute(new CreatePurchaseRequestCommand(PurchaseRequestOrigin.USER,
				List.of(new PurchaseRequestItem(productId, BigDecimal.TEN)), requestedBy));
		flushAndClear();

		PurchaseRequest persisted = purchaseRequestRepositoryAdapter.findById(id).orElseThrow();
		assertThat(persisted.getOrigin()).isEqualTo(PurchaseRequestOrigin.USER);
		assertThat(persisted.getStatus()).isEqualTo(PurchaseRequestStatus.OPEN);
		assertThat(persisted.getRequestedBy()).isEqualTo(requestedBy);
		assertThat(persisted.getItems()).hasSize(1);
		assertThat(persisted.getItems().get(0).productId()).isEqualTo(productId);
		assertThat(persisted.getItems().get(0).quantity()).isEqualByComparingTo("10");
	}

	@Test
	@DisplayName("Persists a minimum-stock triggered request without a requester")
	void creatingAMinStockTriggeredRequestPersistsWithoutARequester() {
		var id = service.execute(new CreatePurchaseRequestCommand(PurchaseRequestOrigin.MIN_STOCK_TRIGGER,
				List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)), null));
		flushAndClear();

		PurchaseRequest persisted = purchaseRequestRepositoryAdapter.findById(id).orElseThrow();
		assertThat(persisted.getOrigin()).isEqualTo(PurchaseRequestOrigin.MIN_STOCK_TRIGGER);
		assertThat(persisted.getRequestedBy()).isNull();
	}

	@Test
	@DisplayName("Persists a sales-order demand request without a requester")
	void creatingASalesOrderDemandRequestPersistsWithoutARequester() {
		var id = service.execute(new CreatePurchaseRequestCommand(PurchaseRequestOrigin.SALES_ORDER_DEMAND,
				List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)), null));
		flushAndClear();

		PurchaseRequest persisted = purchaseRequestRepositoryAdapter.findById(id).orElseThrow();
		assertThat(persisted.getOrigin()).isEqualTo(PurchaseRequestOrigin.SALES_ORDER_DEMAND);
	}

	@Test
	@DisplayName("Rejects an empty item list before anything is persisted")
	void anEmptyItemListIsRejectedBeforeAnythingIsPersisted() {
		assertThatThrownBy(() -> service.execute(
				new CreatePurchaseRequestCommand(PurchaseRequestOrigin.USER, List.of(), UUID.randomUUID())))
				.isInstanceOf(BusinessRuleException.class);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
