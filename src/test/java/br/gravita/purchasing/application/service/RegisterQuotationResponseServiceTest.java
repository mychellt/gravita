package br.gravita.purchasing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.domain.purchasing.QuotationItem;
import br.gravita.core.domain.purchasing.QuotationItemPrice;
import br.gravita.core.domain.purchasing.QuotationNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.RegisterQuotationResponseCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.QuotationRepositoryPort;
import br.gravita.core.usercases.purchasing.RegisterQuotationResponseService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterQuotationResponseServiceTest {

	@Mock
	private QuotationRepositoryPort quotationRepositoryPort;

	private final UUID productId = UUID.randomUUID();
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());

	@Test
	void registeringAResponseFromASentSupplierSavesTheUpdatedQuotation() {
		QuotationId quotationId = QuotationId.of(UUID.randomUUID());
		Quotation quotation = sentQuotation(quotationId);
		when(quotationRepositoryPort.findById(quotationId)).thenReturn(Optional.of(quotation));
		when(quotationRepositoryPort.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));
		RegisterQuotationResponseService service = new RegisterQuotationResponseService(quotationRepositoryPort);

		service.execute(new RegisterQuotationResponseCommand(quotationId, supplierId,
				List.of(new QuotationItemPrice(productId, BigDecimal.TEN)), LocalDate.now().plusDays(7)));

		ArgumentCaptor<Quotation> saved = ArgumentCaptor.forClass(Quotation.class);
		verify(quotationRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getResponses()).hasSize(1);
		assertThat(saved.getValue().getResponses().get(0).supplierId()).isEqualTo(supplierId);
	}

	@Test
	void registeringAResponseForAMissingQuotationIsRejected() {
		QuotationId quotationId = QuotationId.of(UUID.randomUUID());
		when(quotationRepositoryPort.findById(quotationId)).thenReturn(Optional.empty());
		RegisterQuotationResponseService service = new RegisterQuotationResponseService(quotationRepositoryPort);

		assertThatThrownBy(() -> service.execute(new RegisterQuotationResponseCommand(quotationId, supplierId,
				List.of(new QuotationItemPrice(productId, BigDecimal.TEN)), LocalDate.now())))
				.isInstanceOf(QuotationNotFoundException.class);
		verify(quotationRepositoryPort, never()).save(any());
	}

	@Test
	void registeringAResponseFromASupplierNotSentTheQuotationIsRejectedWithoutSaving() {
		QuotationId quotationId = QuotationId.of(UUID.randomUUID());
		Quotation quotation = sentQuotation(quotationId);
		when(quotationRepositoryPort.findById(quotationId)).thenReturn(Optional.of(quotation));
		RegisterQuotationResponseService service = new RegisterQuotationResponseService(quotationRepositoryPort);
		SupplierId strangerSupplier = SupplierId.of(UUID.randomUUID());

		assertThatThrownBy(() -> service.execute(new RegisterQuotationResponseCommand(quotationId, strangerSupplier,
				List.of(new QuotationItemPrice(productId, BigDecimal.TEN)), LocalDate.now())))
				.isInstanceOf(BusinessRuleException.class);
		verify(quotationRepositoryPort, never()).save(any());
	}

	private Quotation sentQuotation(QuotationId id) {
		return Quotation.send(id, PurchaseRequestId.of(UUID.randomUUID()),
				List.of(new QuotationItem(productId, BigDecimal.TEN)), List.of(supplierId));
	}
}
