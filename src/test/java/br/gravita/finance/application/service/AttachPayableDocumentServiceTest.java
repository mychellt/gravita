package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.AttachPayableDocumentCommand;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort.Document;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort.DocumentStorageUnavailableException;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.usercases.finance.AttachPayableDocumentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttachPayableDocumentServiceTest {

	private static final String URL = "https://files.example.com/nf-123.pdf";

	@Mock
	private PayableRepositoryPort payableRepositoryPort;

	@Mock
	private DocumentAttachmentStoragePort documentAttachmentStoragePort;

	@InjectMocks
	private AttachPayableDocumentService service;

	@BeforeEach
	void echoSavedPayable() {
		org.mockito.Mockito.lenient().when(payableRepositoryPort.save(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	private Payable found(PayableStatus status, List<String> attachments) {
		Payable payable = Payable.of(PayableId.of(UUID.randomUUID()), UUID.randomUUID(), PayableOrigin.MANUAL,
				new BigDecimal("250.00"), LocalDate.now().plusDays(5), null, status, null, null, null,
				LedgerScope.NONE, null, attachments);
		when(payableRepositoryPort.findById(payable.getId())).thenReturn(Optional.of(payable));
		return payable;
	}

	private static AttachPayableDocumentCommand command(Payable payable) {
		return new AttachPayableDocumentCommand(payable.getId().value(),
				new AttachPayableDocumentCommand.File("nf-123.pdf", "application/pdf", new byte[] { 1, 2, 3 }));
	}

	@Test
	@DisplayName("Stores the file and appends its reference to the payable")
	void storesTheFileAndAppendsItsReferenceToThePayable() {
		Payable payable = found(PayableStatus.OPEN, null);
		when(documentAttachmentStoragePort.store(any())).thenReturn(URL);

		Payable updated = service.execute(command(payable));

		assertThat(updated.getAttachments()).containsExactly(URL);
		verify(payableRepositoryPort).save(updated);

		ArgumentCaptor<Document> document = ArgumentCaptor.forClass(Document.class);
		verify(documentAttachmentStoragePort).store(document.capture());
		assertThat(document.getValue().fileName()).isEqualTo("nf-123.pdf");
		assertThat(document.getValue().contentType()).isEqualTo("application/pdf");
		assertThat(document.getValue().content()).containsExactly(1, 2, 3);
	}

	@Test
	@DisplayName("Keeps the attachments already linked to the payable")
	void keepsTheAttachmentsAlreadyLinked() {
		Payable payable = found(PayableStatus.PAID, List.of("https://files.example.com/boleto.pdf"));
		when(documentAttachmentStoragePort.store(any())).thenReturn(URL);

		Payable updated = service.execute(command(payable));

		assertThat(updated.getAttachments()).containsExactly("https://files.example.com/boleto.pdf", URL);
		assertThat(updated.getStatus()).isEqualTo(PayableStatus.PAID);
	}

	@Test
	@DisplayName("Rejects an unknown payable without storing the file")
	void anUnknownPayableIsRejectedWithoutStoringTheFile() {
		UUID id = UUID.randomUUID();
		when(payableRepositoryPort.findById(PayableId.of(id))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new AttachPayableDocumentCommand(id,
				new AttachPayableDocumentCommand.File("a.pdf", "application/pdf", new byte[] { 1 }))))
				.isInstanceOf(ResourceNotFoundException.class);

		verifyNoInteractions(documentAttachmentStoragePort);
		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Leaves the payable untouched when storing the file fails")
	void aStorageFailureLeavesThePayableUntouched() {
		Payable payable = found(PayableStatus.OPEN, null);
		when(documentAttachmentStoragePort.store(any()))
				.thenThrow(new DocumentStorageUnavailableException("storage down"));

		assertThatThrownBy(() -> service.execute(command(payable)))
				.isInstanceOf(DocumentStorageUnavailableException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a command carrying an empty file")
	void theCommandRejectsAnEmptyFile() {
		assertThatThrownBy(() -> new AttachPayableDocumentCommand.File("a.pdf", "application/pdf", new byte[0]))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> new AttachPayableDocumentCommand.File(" ", "application/pdf", new byte[] { 1 }))
				.isInstanceOf(BusinessRuleException.class);
	}
}
