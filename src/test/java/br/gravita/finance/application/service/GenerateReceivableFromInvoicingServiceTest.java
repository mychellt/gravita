package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingCommand;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingCommand.Installment;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.usercases.finance.GenerateReceivableFromInvoicingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerateReceivableFromInvoicingServiceTest {

	private static final LocalDate FIRST_DUE = LocalDate.of(2026, 10, 30);

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@InjectMocks
	private GenerateReceivableFromInvoicingService service;

	@Test
	@DisplayName("Creates one open receivable per installment referencing the fiscal document")
	void createsOneOpenInvoicingReceivablePerInstallmentReferencingTheFiscalDocument() {
		UUID customerId = UUID.randomUUID();
		UUID documentId = UUID.randomUUID();
		when(receivableRepositoryPort.findByOriginDocumentRef(documentId)).thenReturn(List.of());
		when(receivableRepositoryPort.save(any(Receivable.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		List<Receivable> created = service.execute(new GenerateReceivableFromInvoicingCommand(customerId,
				documentId, List.of(new Installment(FIRST_DUE, new BigDecimal("100.00")),
						new Installment(FIRST_DUE.plusDays(30), new BigDecimal("100.00")),
						new Installment(FIRST_DUE.plusDays(60), new BigDecimal("50.00")))));

		assertThat(created).hasSize(3);
		assertThat(created).allSatisfy(receivable -> {
			assertThat(receivable.getOrigin()).isEqualTo(ReceivableOrigin.INVOICING);
			assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.OPEN);
			assertThat(receivable.getCustomerId()).isEqualTo(customerId);
			assertThat(receivable.getOriginDocumentRef()).isEqualTo(documentId);
			assertThat(receivable.getInstallments()).isEqualTo(3);
		});
		assertThat(created).extracting(Receivable::getInstallmentNumber).containsExactly(1, 2, 3);
		assertThat(created).extracting(Receivable::getDueDate).containsExactly(FIRST_DUE,
				FIRST_DUE.plusDays(30), FIRST_DUE.plusDays(60));
		assertThat(created).extracting(Receivable::getAmount).usingComparatorForType(
				BigDecimal::compareTo, BigDecimal.class).containsExactly(new BigDecimal("100.00"),
						new BigDecimal("100.00"), new BigDecimal("50.00"));
		verify(receivableRepositoryPort, times(3)).save(any(Receivable.class));
	}

	@Test
	@DisplayName("Is idempotent per origin document and returns the receivables that already exist")
	void isIdempotentPerOriginDocumentAndReturnsTheExistingReceivables() {
		UUID customerId = UUID.randomUUID();
		UUID documentId = UUID.randomUUID();
		Receivable existing = Receivable.createFromInvoicing(ReceivableId.of(UUID.randomUUID()), customerId,
				documentId, BigDecimal.TEN, FIRST_DUE, 1, 1);
		when(receivableRepositoryPort.findByOriginDocumentRef(documentId)).thenReturn(List.of(existing));

		List<Receivable> result = service.execute(new GenerateReceivableFromInvoicingCommand(customerId,
				documentId, List.of(new Installment(FIRST_DUE, BigDecimal.TEN))));

		assertThat(result).containsExactly(existing);
		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a command without installments and saves nothing")
	void rejectsACommandWithoutInstallmentsWithoutSavingAnything() {
		UUID documentId = UUID.randomUUID();

		assertThatThrownBy(() -> service
				.execute(new GenerateReceivableFromInvoicingCommand(UUID.randomUUID(), documentId, List.of())))
				.isInstanceOf(BusinessRuleException.class);

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects an installment amount that is zero or negative")
	void rejectsANonPositiveInstallmentAmount() {
		UUID documentId = UUID.randomUUID();
		when(receivableRepositoryPort.findByOriginDocumentRef(documentId)).thenReturn(List.of());

		assertThatThrownBy(() -> service.execute(new GenerateReceivableFromInvoicingCommand(UUID.randomUUID(),
				documentId, List.of(new Installment(FIRST_DUE, BigDecimal.ZERO)))))
				.isInstanceOf(BusinessRuleException.class);

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Saves each receivable with its own id")
	void savesEachReceivableWithItsOwnId() {
		UUID documentId = UUID.randomUUID();
		when(receivableRepositoryPort.findByOriginDocumentRef(documentId)).thenReturn(List.of());
		when(receivableRepositoryPort.save(any(Receivable.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new GenerateReceivableFromInvoicingCommand(UUID.randomUUID(), documentId,
				List.of(new Installment(FIRST_DUE, BigDecimal.ONE), new Installment(FIRST_DUE, BigDecimal.ONE))));

		ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort, times(2)).save(saved.capture());
		assertThat(saved.getAllValues()).extracting(Receivable::getId).doesNotHaveDuplicates();
	}
}
