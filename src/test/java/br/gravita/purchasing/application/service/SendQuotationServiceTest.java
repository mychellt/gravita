package br.gravita.purchasing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import br.gravita.core.domain.purchasing.PurchaseRequestNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.SendQuotationCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.QuotationRepositoryPort;
import br.gravita.core.usercases.purchasing.SendQuotationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendQuotationServiceTest {

	@Mock
	private PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;

	@Mock
	private QuotationRepositoryPort quotationRepositoryPort;

	@Test
	@DisplayName("Sending a quotation for an open request creates it and moves the request to QUOTED")
	void sendingAQuotationForAnOpenRequestCreatesItAndTransitionsTheRequestToQuoted() {
		PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
		List<PurchaseRequestItem> items = List.of(
				new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.TEN),
				new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE));
		PurchaseRequest openRequest = PurchaseRequest.open(requestId, PurchaseRequestOrigin.USER, items,
				UUID.randomUUID());
		List<SupplierId> suppliers = List.of(SupplierId.of(UUID.randomUUID()), SupplierId.of(UUID.randomUUID()));
		when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(openRequest));
		when(quotationRepositoryPort.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));
		SendQuotationService service = new SendQuotationService(purchaseRequestRepositoryPort, quotationRepositoryPort);

		var id = service.execute(new SendQuotationCommand(requestId, suppliers));

		assertThat(id).isNotNull();
		ArgumentCaptor<Quotation> savedQuotation = ArgumentCaptor.forClass(Quotation.class);
		verify(quotationRepositoryPort).save(savedQuotation.capture());
		assertThat(savedQuotation.getValue().getRequestId()).isEqualTo(requestId);
		assertThat(savedQuotation.getValue().getSuppliers()).isEqualTo(suppliers);
		assertThat(savedQuotation.getValue().getItems()).hasSize(2);
		assertThat(savedQuotation.getValue().getResponses()).isEmpty();

		ArgumentCaptor<PurchaseRequest> savedRequest = ArgumentCaptor.forClass(PurchaseRequest.class);
		verify(purchaseRequestRepositoryPort).save(savedRequest.capture());
		assertThat(savedRequest.getValue().getStatus()).isEqualTo(PurchaseRequestStatus.QUOTED);
	}

	@Test
	@DisplayName("Rejects sending a quotation for a missing request")
	void sendingAQuotationForAMissingRequestIsRejected() {
		PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
		when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.empty());
		SendQuotationService service = new SendQuotationService(purchaseRequestRepositoryPort, quotationRepositoryPort);

		assertThatThrownBy(() -> service.execute(
				new SendQuotationCommand(requestId, List.of(SupplierId.of(UUID.randomUUID())))))
				.isInstanceOf(PurchaseRequestNotFoundException.class);
	}

	@Test
	@DisplayName("Rejects sending a quotation for a request that is not open")
	void sendingAQuotationForARequestThatIsNotOpenIsRejected() {
		PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
		PurchaseRequest quotedRequest = PurchaseRequest.of(requestId, PurchaseRequestOrigin.USER,
				List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)), UUID.randomUUID(),
				PurchaseRequestStatus.QUOTED);
		when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(quotedRequest));
		SendQuotationService service = new SendQuotationService(purchaseRequestRepositoryPort, quotationRepositoryPort);

		assertThatThrownBy(() -> service.execute(
				new SendQuotationCommand(requestId, List.of(SupplierId.of(UUID.randomUUID())))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("OPEN");
		verify(quotationRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects sending a quotation without any supplier")
	void sendingAQuotationWithoutAnySupplierIsRejected() {
		PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
		PurchaseRequest openRequest = PurchaseRequest.open(requestId, PurchaseRequestOrigin.USER,
				List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)), UUID.randomUUID());
		when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(openRequest));
		SendQuotationService service = new SendQuotationService(purchaseRequestRepositoryPort, quotationRepositoryPort);

		assertThatThrownBy(() -> service.execute(new SendQuotationCommand(requestId, List.of())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one supplier");
	}
}
