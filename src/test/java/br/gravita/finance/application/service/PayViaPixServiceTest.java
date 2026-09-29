package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.BankIntegrationUnavailableException;
import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.PayViaPixCommand;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.PixPaymentReceipt;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.PixPaymentRequest;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort.Document;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort.DocumentStorageUnavailableException;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.usercases.finance.PayViaPixService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayViaPixServiceTest {

	private static final String PIX_KEY = "fornecedor@example.com";
	private static final String RECEIPT_URL = "https://files.example.com/pix-receipt-E2E123.pdf";
	private static final PixPaymentReceipt RECEIPT = new PixPaymentReceipt("E2E123", "application/pdf",
			new byte[] { 1, 2, 3 });

	@Mock
	private PayableRepositoryPort payableRepositoryPort;

	@Mock
	private BankIntegrationPort bankIntegrationPort;

	@Mock
	private DocumentAttachmentStoragePort documentAttachmentStoragePort;

	@InjectMocks
	private PayViaPixService service;

	@BeforeEach
	void echoSavedPayable() {
		org.mockito.Mockito.lenient().when(payableRepositoryPort.save(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	private Payable payable(PayableStatus status) {
		return Payable.of(PayableId.of(UUID.randomUUID()), UUID.randomUUID(), PayableOrigin.MANUAL,
				new BigDecimal("250.00"), LocalDate.now().plusDays(5), null, status, null, null, null,
				new LedgerScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()), UUID.randomUUID(),
				List.of("https://files.example.com/boleto.pdf"));
	}

	private Payable found(PayableStatus status) {
		Payable payable = payable(status);
		when(payableRepositoryPort.findById(payable.getId())).thenReturn(Optional.of(payable));
		return payable;
	}

	private PayViaPixCommand command(Payable payable) {
		return new PayViaPixCommand(payable.getId().value(), PIX_KEY);
	}

	@Test
	void transfersToTheRecipientMarksThePayablePaidAndAttachesTheReceipt() {
		Payable payable = found(PayableStatus.APPROVED);
		when(bankIntegrationPort.payViaPix(any())).thenReturn(RECEIPT);
		when(documentAttachmentStoragePort.store(any())).thenReturn(RECEIPT_URL);

		Payable paid = service.execute(command(payable));

		assertThat(paid.getStatus()).isEqualTo(PayableStatus.PAID);
		assertThat(paid.getAttachments()).containsExactly("https://files.example.com/boleto.pdf", RECEIPT_URL);
		verify(payableRepositoryPort).save(paid);

		ArgumentCaptor<PixPaymentRequest> request = ArgumentCaptor.forClass(PixPaymentRequest.class);
		verify(bankIntegrationPort).payViaPix(request.capture());
		assertThat(request.getValue().payableId()).isEqualTo(payable.getId().value());
		assertThat(request.getValue().pixKey()).isEqualTo(PIX_KEY);
		assertThat(request.getValue().amount()).isEqualByComparingTo("250.00");
		assertThat(request.getValue().scope()).isEqualTo(payable.getScope());

		ArgumentCaptor<Document> document = ArgumentCaptor.forClass(Document.class);
		verify(documentAttachmentStoragePort).store(document.capture());
		assertThat(document.getValue().fileName()).isEqualTo("pix-receipt-E2E123.pdf");
		assertThat(document.getValue().contentType()).isEqualTo("application/pdf");
		assertThat(document.getValue().content()).containsExactly(1, 2, 3);
	}

	@Test
	void aTransferFailureLeavesThePayableApprovedAndStoresNothing() {
		Payable payable = found(PayableStatus.APPROVED);
		when(bankIntegrationPort.payViaPix(any()))
				.thenThrow(new BankIntegrationUnavailableException("bank down"));

		assertThatThrownBy(() -> service.execute(command(payable)))
				.isInstanceOf(BankIntegrationUnavailableException.class);

		verify(payableRepositoryPort, never()).save(any());
		verifyNoInteractions(documentAttachmentStoragePort);
	}

	@Test
	void aTransferTheBankRefusesLeavesThePayableApproved() {
		Payable payable = found(PayableStatus.APPROVED);
		when(bankIntegrationPort.payViaPix(any())).thenThrow(new BusinessRuleException("unknown PIX key"));

		assertThatThrownBy(() -> service.execute(command(payable))).isInstanceOf(BusinessRuleException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void aReceiptThatCannotBeStoredStillRecordsThePaymentSoItIsNeverPaidTwice() {
		Payable payable = found(PayableStatus.APPROVED);
		when(bankIntegrationPort.payViaPix(any())).thenReturn(RECEIPT);
		when(documentAttachmentStoragePort.store(any()))
				.thenThrow(new DocumentStorageUnavailableException("storage down"));

		Payable paid = service.execute(command(payable));

		assertThat(paid.getStatus()).isEqualTo(PayableStatus.PAID);
		assertThat(paid.getAttachments()).containsExactly("https://files.example.com/boleto.pdf");
		verify(payableRepositoryPort).save(paid);
	}

	@Test
	void onlyApprovedPayablesCanBePaidAndTheBankIsNotContactedOtherwise() {
		for (PayableStatus status : List.of(PayableStatus.OPEN, PayableStatus.PAID, PayableStatus.CANCELLED)) {
			Payable payable = found(status);

			assertThatThrownBy(() -> service.execute(command(payable))).isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("Only APPROVED payables can be paid via PIX")
					.hasMessageContaining(status.name());
		}

		verifyNoInteractions(bankIntegrationPort, documentAttachmentStoragePort);
		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void anUnknownPayableIsNotFound() {
		UUID id = UUID.randomUUID();
		when(payableRepositoryPort.findById(PayableId.of(id))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new PayViaPixCommand(id, PIX_KEY)))
				.isInstanceOf(ResourceNotFoundException.class);

		verifyNoInteractions(bankIntegrationPort);
	}

	@Test
	void theCommandRequiresAPayableAndANonBlankPixKey() {
		assertThatThrownBy(() -> new PayViaPixCommand(null, PIX_KEY)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new PayViaPixCommand(UUID.randomUUID(), null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new PayViaPixCommand(UUID.randomUUID(), "  "))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
