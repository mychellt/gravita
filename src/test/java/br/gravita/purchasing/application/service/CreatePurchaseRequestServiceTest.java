package br.gravita.purchasing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import br.gravita.core.usercases.purchasing.CreatePurchaseRequestService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreatePurchaseRequestServiceTest {

	@Mock
	private PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;

	@Test
	void shouldCreateAManualRequestWithAnArbitraryItemList() {
		when(purchaseRequestRepositoryPort.save(any(PurchaseRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		CreatePurchaseRequestService service = new CreatePurchaseRequestService(purchaseRequestRepositoryPort);
		UUID requestedBy = UUID.randomUUID();
		List<PurchaseRequestItem> items = List.of(
				new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.TEN),
				new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE));

		var id = service.execute(new CreatePurchaseRequestCommand(PurchaseRequestOrigin.USER, items, requestedBy));

		assertThat(id).isNotNull();
		ArgumentCaptor<PurchaseRequest> saved = ArgumentCaptor.forClass(PurchaseRequest.class);
		verify(purchaseRequestRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(id);
		assertThat(saved.getValue().getOrigin()).isEqualTo(PurchaseRequestOrigin.USER);
		assertThat(saved.getValue().getRequestedBy()).isEqualTo(requestedBy);
		assertThat(saved.getValue().getItems()).isEqualTo(items);
		assertThat(saved.getValue().getStatus()).isEqualTo(PurchaseRequestStatus.OPEN);
	}

	@Test
	void shouldCreateARequestFromInventorysMinStockTriggerWithoutARequester() {
		when(purchaseRequestRepositoryPort.save(any(PurchaseRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		CreatePurchaseRequestService service = new CreatePurchaseRequestService(purchaseRequestRepositoryPort);
		List<PurchaseRequestItem> items = List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.TEN));

		var id = service.execute(
				new CreatePurchaseRequestCommand(PurchaseRequestOrigin.MIN_STOCK_TRIGGER, items, null));

		ArgumentCaptor<PurchaseRequest> saved = ArgumentCaptor.forClass(PurchaseRequest.class);
		verify(purchaseRequestRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getOrigin()).isEqualTo(PurchaseRequestOrigin.MIN_STOCK_TRIGGER);
		assertThat(saved.getValue().getRequestedBy()).isNull();
		assertThat(id).isNotNull();
	}

	@Test
	void shouldCreateARequestFromSalesOrderDemandWithoutARequester() {
		when(purchaseRequestRepositoryPort.save(any(PurchaseRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		CreatePurchaseRequestService service = new CreatePurchaseRequestService(purchaseRequestRepositoryPort);
		List<PurchaseRequestItem> items = List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.TEN));

		var id = service.execute(
				new CreatePurchaseRequestCommand(PurchaseRequestOrigin.SALES_ORDER_DEMAND, items, null));

		ArgumentCaptor<PurchaseRequest> saved = ArgumentCaptor.forClass(PurchaseRequest.class);
		verify(purchaseRequestRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getOrigin()).isEqualTo(PurchaseRequestOrigin.SALES_ORDER_DEMAND);
		assertThat(id).isNotNull();
	}

	@Test
	void shouldRejectAnEmptyItemList() {
		CreatePurchaseRequestService service = new CreatePurchaseRequestService(purchaseRequestRepositoryPort);

		assertThatThrownBy(() -> service.execute(
				new CreatePurchaseRequestCommand(PurchaseRequestOrigin.USER, List.of(), UUID.randomUUID())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one item");
	}

	@Test
	void shouldRejectAUserOriginRequestWithoutARequester() {
		CreatePurchaseRequestService service = new CreatePurchaseRequestService(purchaseRequestRepositoryPort);
		List<PurchaseRequestItem> items = List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.TEN));

		assertThatThrownBy(() -> service.execute(new CreatePurchaseRequestCommand(PurchaseRequestOrigin.USER, items, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("requestedBy is required");
	}

	@Test
	void shouldRejectASystemTriggeredRequestThatCarriesARequester() {
		CreatePurchaseRequestService service = new CreatePurchaseRequestService(purchaseRequestRepositoryPort);
		List<PurchaseRequestItem> items = List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.TEN));

		assertThatThrownBy(() -> service.execute(new CreatePurchaseRequestCommand(
				PurchaseRequestOrigin.MIN_STOCK_TRIGGER, items, UUID.randomUUID())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("must be null for system-triggered origin");
	}
}
