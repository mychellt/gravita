package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.AttachmentFile;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableAttachment;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.ports.inbound.finance.AttachPayableDocumentCommand;
import br.gravita.core.ports.outbound.persistence.finance.DocumentAttachmentStoragePort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.usercases.finance.AttachPayableDocumentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttachPayableDocumentServiceTest {

	@Mock
	private PayableRepositoryPort payableRepositoryPort;

	@Mock
	private DocumentAttachmentStoragePort documentAttachmentStoragePort;

	@InjectMocks
	private AttachPayableDocumentService service;

	private Payable existingPayable() {
		Payable payable = Payable.createManual(PayableId.of(UUID.randomUUID()), null, new BigDecimal("500.00"),
				LocalDate.now().plusDays(10), null);
		when(payableRepositoryPort.findById(payable.getId())).thenReturn(Optional.of(payable));
		return payable;
	}

	private static AttachmentFile file(String name) {
		return new AttachmentFile(name, "application/pdf", new byte[] { 1, 2, 3 });
	}

	@Test
	void storesTheFileAndSavesThePayableWithTheReferenceAppended() {
		Payable payable = existingPayable();
		AttachmentFile file = file("boleto.pdf");
		PayableAttachment stored = new PayableAttachment("ref-1", "boleto.pdf", "application/pdf", 3);
		when(documentAttachmentStoragePort.store(payable.getId(), file)).thenReturn(stored);
		when(payableRepositoryPort.save(any(Payable.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Payable result = service.execute(new AttachPayableDocumentCommand(payable.getId().value(), file));

		ArgumentCaptor<Payable> saved = ArgumentCaptor.forClass(Payable.class);
		verify(payableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(payable.getId());
		assertThat(saved.getValue().getAttachments()).containsExactly(stored);
		assertThat(result.getAttachments()).containsExactly(stored);
	}

	@Test
	void keepsTheAttachmentsAlreadyLinked() {
		Payable payable = existingPayable();
		PayableAttachment first = new PayableAttachment("ref-1", "boleto.pdf", "application/pdf", 3);
		PayableAttachment second = new PayableAttachment("ref-2", "nf.pdf", "application/pdf", 3);
		when(payableRepositoryPort.findById(payable.getId())).thenReturn(Optional.of(payable.withAttachment(first)));
		when(documentAttachmentStoragePort.store(any(), any())).thenReturn(second);
		when(payableRepositoryPort.save(any(Payable.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Payable result = service.execute(new AttachPayableDocumentCommand(payable.getId().value(), file("nf.pdf")));

		assertThat(result.getAttachments()).containsExactly(first, second);
	}

	@Test
	void rejectsAnUnknownPayableWithoutStoringTheFile() {
		PayableId id = PayableId.of(UUID.randomUUID());
		when(payableRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new AttachPayableDocumentCommand(id.value(), file("boleto.pdf"))))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(documentAttachmentStoragePort, never()).store(any(), any());
		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void theCommandRequiresPayableIdAndFile() {
		assertThatThrownBy(() -> new AttachPayableDocumentCommand(null, file("a.pdf")))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("payableId");
		assertThatThrownBy(() -> new AttachPayableDocumentCommand(UUID.randomUUID(), null))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("file");
	}
}
