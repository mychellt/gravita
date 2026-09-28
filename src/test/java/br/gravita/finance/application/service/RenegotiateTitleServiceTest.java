package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.finance.SettlementMethod;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.RenegotiateTitleCommand;
import br.gravita.core.ports.inbound.finance.RenegotiateTitleCommand.Installment;
import br.gravita.core.ports.outbound.finance.UpdateCustomerCreditStatusPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.RenegotiationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.usercases.finance.RenegotiateTitleService;
import java.math.BigDecimal;
import java.time.Instant;
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
class RenegotiateTitleServiceTest {

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Mock
	private SettlementRepositoryPort settlementRepositoryPort;

	@Mock
	private RenegotiationRepositoryPort renegotiationRepositoryPort;

	@Mock
	private UpdateCustomerCreditStatusPort updateCustomerCreditStatusPort;

	@InjectMocks
	private RenegotiateTitleService service;

	private final UUID customerId = UUID.randomUUID();
	private final LocalDate today = LocalDate.now();

	@BeforeEach
	void echoSaves() {
		lenient().when(receivableRepositoryPort.save(any(Receivable.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(renegotiationRepositoryPort.save(any(Renegotiation.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	private Receivable receivable(UUID customer, ReceivableStatus status, LocalDate dueDate, String amount) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), customer, ReceivableOrigin.INVOICING,
				new BigDecimal(amount), dueDate, null, status, UUID.randomUUID(), 1);
	}

	private Receivable overdue(String amount) {
		return found(unstubbedOverdue(amount));
	}

	private Receivable unstubbedOverdue(String amount) {
		return receivable(customerId, ReceivableStatus.OPEN, today.minusDays(10), amount);
	}

	private Receivable found(Receivable receivable) {
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		return receivable;
	}

	private RenegotiateTitleCommand command(List<Receivable> originals, Installment... plan) {
		return new RenegotiateTitleCommand(originals.stream().map(r -> r.getId().value()).toList(), List.of(plan));
	}

	private List<Receivable> savedReceivables() {
		ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort, atLeastOnce()).save(saved.capture());
		return saved.getAllValues();
	}

	@Test
	void marksTheOriginalsRenegotiatedAndCreatesOneOpenReceivablePerInstallment() {
		Receivable first = overdue("100.00");
		Receivable second = overdue("50.00");
		when(receivableRepositoryPort.findOutstandingByCustomerId(customerId)).thenReturn(List.of());

		Renegotiation renegotiation = service.execute(command(List.of(first, second),
				new Installment(today.plusDays(30), new BigDecimal("80.00")),
				new Installment(today.plusDays(60), new BigDecimal("80.00"))));

		List<Receivable> saved = savedReceivables();
		assertThat(saved).filteredOn(r -> r.getStatus() == ReceivableStatus.RENEGOTIATED)
				.extracting(Receivable::getId).containsExactly(first.getId(), second.getId());
		List<Receivable> created = saved.stream().filter(r -> r.getStatus() == ReceivableStatus.OPEN).toList();
		assertThat(created).hasSize(2);
		assertThat(created).allSatisfy(r -> {
			assertThat(r.getOrigin()).isEqualTo(ReceivableOrigin.RENEGOTIATION);
			assertThat(r.getCustomerId()).isEqualTo(customerId);
			assertThat(r.getInstallments()).isEqualTo(2);
		});
		assertThat(created).extracting(Receivable::getDueDate).containsExactly(today.plusDays(30),
				today.plusDays(60));
		assertThat(created).extracting(Receivable::getAmount).usingElementComparator(BigDecimal::compareTo)
				.containsExactly(new BigDecimal("80.00"), new BigDecimal("80.00"));
		assertThat(created).extracting(Receivable::getInstallmentNumber).containsExactly(1, 2);
		assertThat(renegotiation.getNewReceivableIds()).containsExactlyElementsOf(
				created.stream().map(Receivable::getId).toList());
	}

	@Test
	void theRenegotiationLinksBackToTheOriginalTitles() {
		Receivable first = overdue("100.00");
		Receivable second = overdue("50.00");
		when(receivableRepositoryPort.findOutstandingByCustomerId(customerId)).thenReturn(List.of());

		Renegotiation renegotiation = service.execute(command(List.of(first, second),
				new Installment(today.plusDays(30), new BigDecimal("150.00"))));

		assertThat(renegotiation.getCustomerId()).isEqualTo(customerId);
		assertThat(renegotiation.getOriginalReceivableIds()).containsExactly(first.getId(), second.getId());
		verify(renegotiationRepositoryPort).save(renegotiation);
	}

	@Test
	void refreshesTheCustomersCreditStatusFromWhatIsStillOwed() {
		Receivable original = overdue("100.00");
		Receivable outstandingNew = receivable(customerId, ReceivableStatus.OPEN, today.plusDays(30), "100.00");
		Receivable partiallyPaid = receivable(customerId, ReceivableStatus.PARTIALLY_SETTLED, today.plusDays(10),
				"200.00");
		when(receivableRepositoryPort.findOutstandingByCustomerId(customerId))
				.thenReturn(List.of(outstandingNew, partiallyPaid));
		when(settlementRepositoryPort.findByReceivableId(outstandingNew.getId())).thenReturn(List.of());
		when(settlementRepositoryPort.findByReceivableId(partiallyPaid.getId())).thenReturn(List.of(Settlement.of(
				SettlementId.of(UUID.randomUUID()), partiallyPaid.getId(), new BigDecimal("50.00"), null, null,
				new BigDecimal("10.00"), null, SettlementMethod.MANUAL,
				Instant.now())));

		service.execute(command(List.of(original), new Installment(today.plusDays(30), new BigDecimal("100.00"))));

		verify(updateCustomerCreditStatusPort).update(eq(customerId),
				argThat((BigDecimal balance) -> balance.compareTo(new BigDecimal("240.00")) == 0),
				eq(CustomerStatus.REGULAR));
	}

	@Test
	void theCustomerStaysDelinquentWhileAnotherTitleIsStillOverdue() {
		Receivable original = overdue("100.00");
		Receivable stillOverdue = receivable(customerId, ReceivableStatus.OPEN, today.minusDays(3), "70.00");
		when(receivableRepositoryPort.findOutstandingByCustomerId(customerId)).thenReturn(List.of(stillOverdue));
		when(settlementRepositoryPort.findByReceivableId(stillOverdue.getId())).thenReturn(List.of());

		service.execute(command(List.of(original), new Installment(today.plusDays(30), new BigDecimal("100.00"))));

		verify(updateCustomerCreditStatusPort).update(eq(customerId),
				argThat((BigDecimal balance) -> balance.compareTo(new BigDecimal("70.00")) == 0),
				eq(CustomerStatus.DELINQUENT));
	}

	@Test
	void rejectsATitleThatIsNotOverdueWithoutWritingAnything() {
		Receivable notDue = found(receivable(customerId, ReceivableStatus.OPEN, today.plusDays(5), "100.00"));

		assertThatThrownBy(() -> service.execute(
				command(List.of(notDue), new Installment(today.plusDays(30), new BigDecimal("100.00")))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("not overdue");

		verify(receivableRepositoryPort, never()).save(any());
		verifyNoInteractions(renegotiationRepositoryPort, updateCustomerCreditStatusPort);
	}

	@Test
	void rejectsAnAlreadyRenegotiatedTitle() {
		Receivable done = found(receivable(customerId, ReceivableStatus.RENEGOTIATED, today.minusDays(5), "100.00"));

		assertThatThrownBy(() -> service.execute(
				command(List.of(done), new Installment(today.plusDays(30), new BigDecimal("100.00")))))
				.isInstanceOf(BusinessRuleException.class);

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsAnUnknownReceivable() {
		UUID unknown = UUID.randomUUID();
		when(receivableRepositoryPort.findById(ReceivableId.of(unknown))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new RenegotiateTitleCommand(List.of(unknown),
				List.of(new Installment(today.plusDays(30), BigDecimal.TEN)))))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsTitlesOfDifferentCustomers() {
		Receivable mine = overdue("100.00");
		Receivable other = found(receivable(UUID.randomUUID(), ReceivableStatus.OPEN, today.minusDays(4), "10.00"));

		assertThatThrownBy(() -> service.execute(command(List.of(mine, other),
				new Installment(today.plusDays(30), new BigDecimal("110.00")))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("same customer");

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsTheSameTitleTwice() {
		Receivable original = unstubbedOverdue("100.00");
		RenegotiateTitleCommand twice = new RenegotiateTitleCommand(
				List.of(original.getId().value(), original.getId().value()),
				List.of(new Installment(today.plusDays(30), BigDecimal.TEN)));

		assertThatThrownBy(() -> service.execute(twice)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void requiresAnInstallmentPlanAndAtLeastOneTitle() {
		Receivable original = unstubbedOverdue("100.00");

		assertThatThrownBy(() -> service.execute(command(List.of(original))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("installment");
		assertThatThrownBy(() -> service.execute(new RenegotiateTitleCommand(List.of(),
				List.of(new Installment(today.plusDays(30), BigDecimal.TEN)))))
				.isInstanceOf(BusinessRuleException.class);

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsAnInstallmentThatIsAlreadyDue() {
		Receivable original = overdue("100.00");

		assertThatThrownBy(() -> service.execute(
				command(List.of(original), new Installment(today.minusDays(1), new BigDecimal("100.00")))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("already due");

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsANonPositiveInstallmentAmount() {
		Receivable original = overdue("100.00");

		assertThatThrownBy(() -> service.execute(
				command(List.of(original), new Installment(today.plusDays(30), BigDecimal.ZERO))))
				.isInstanceOf(BusinessRuleException.class);

		verify(receivableRepositoryPort, never()).save(any());
		verify(updateCustomerCreditStatusPort, never()).update(any(), any(), any());
	}
}
