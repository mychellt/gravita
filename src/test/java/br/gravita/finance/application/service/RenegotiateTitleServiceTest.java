package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.RenegotiateTitleCommand;
import br.gravita.core.ports.inbound.finance.RenegotiateTitleCommand.Installment;
import br.gravita.core.ports.outbound.finance.UpdateCustomerCreditStatusPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.RenegotiationRepositoryPort;
import br.gravita.core.usercases.finance.RenegotiateTitleService;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RenegotiateTitleServiceTest {

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

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
	@DisplayName("Marks the original titles as renegotiated and creates one open receivable per installment")
	void marksTheOriginalsRenegotiatedAndCreatesOneOpenReceivablePerInstallment() {
		Receivable first = overdue("100.00");
		Receivable second = overdue("50.00");

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
	@DisplayName("Links the renegotiation back to the original titles")
	void theRenegotiationLinksBackToTheOriginalTitles() {
		Receivable first = overdue("100.00");
		Receivable second = overdue("50.00");

		Renegotiation renegotiation = service.execute(command(List.of(first, second),
				new Installment(today.plusDays(30), new BigDecimal("150.00"))));

		assertThat(renegotiation.getCustomerId()).isEqualTo(customerId);
		assertThat(renegotiation.getOriginalReceivableIds()).containsExactly(first.getId(), second.getId());
		verify(renegotiationRepositoryPort).save(renegotiation);
	}

	@Test
	@DisplayName("Refreshes the customer's credit status once everything is saved")
	void refreshesTheCustomersCreditStatusOnceEverythingIsSaved() {
		Receivable original = overdue("100.00");

		service.execute(command(List.of(original), new Installment(today.plusDays(30), new BigDecimal("100.00"))));

		InOrder inOrder = inOrder(receivableRepositoryPort, renegotiationRepositoryPort,
				updateCustomerCreditStatusPort);
		inOrder.verify(receivableRepositoryPort, atLeastOnce()).save(any(Receivable.class));
		inOrder.verify(renegotiationRepositoryPort).save(any(Renegotiation.class));
		inOrder.verify(updateCustomerCreditStatusPort).update(customerId);
	}

	@Test
	@DisplayName("Rejects a title that is not overdue without writing anything")
	void rejectsATitleThatIsNotOverdueWithoutWritingAnything() {
		Receivable notDue = found(receivable(customerId, ReceivableStatus.OPEN, today.plusDays(5), "100.00"));

		assertThatThrownBy(() -> service.execute(
				command(List.of(notDue), new Installment(today.plusDays(30), new BigDecimal("100.00")))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("not overdue");

		verify(receivableRepositoryPort, never()).save(any());
		verifyNoInteractions(renegotiationRepositoryPort, updateCustomerCreditStatusPort);
	}

	@Test
	@DisplayName("Rejects a title that was already renegotiated")
	void rejectsAnAlreadyRenegotiatedTitle() {
		Receivable done = found(receivable(customerId, ReceivableStatus.RENEGOTIATED, today.minusDays(5), "100.00"));

		assertThatThrownBy(() -> service.execute(
				command(List.of(done), new Installment(today.plusDays(30), new BigDecimal("100.00")))))
				.isInstanceOf(BusinessRuleException.class);

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects an unknown receivable")
	void rejectsAnUnknownReceivable() {
		UUID unknown = UUID.randomUUID();
		when(receivableRepositoryPort.findById(ReceivableId.of(unknown))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new RenegotiateTitleCommand(List.of(unknown),
				List.of(new Installment(today.plusDays(30), BigDecimal.TEN)))))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects titles that belong to different customers")
	void rejectsTitlesOfDifferentCustomers() {
		Receivable mine = overdue("100.00");
		Receivable other = found(receivable(UUID.randomUUID(), ReceivableStatus.OPEN, today.minusDays(4), "10.00"));

		assertThatThrownBy(() -> service.execute(command(List.of(mine, other),
				new Installment(today.plusDays(30), new BigDecimal("110.00")))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("same customer");

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects the same title listed twice")
	void rejectsTheSameTitleTwice() {
		Receivable original = unstubbedOverdue("100.00");
		RenegotiateTitleCommand twice = new RenegotiateTitleCommand(
				List.of(original.getId().value(), original.getId().value()),
				List.of(new Installment(today.plusDays(30), BigDecimal.TEN)));

		assertThatThrownBy(() -> service.execute(twice)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Requires an installment plan and at least one title")
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
	@DisplayName("Rejects an installment whose due date has already passed")
	void rejectsAnInstallmentThatIsAlreadyDue() {
		Receivable original = overdue("100.00");

		assertThatThrownBy(() -> service.execute(
				command(List.of(original), new Installment(today.minusDays(1), new BigDecimal("100.00")))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("already due");

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects an installment amount that is zero or negative")
	void rejectsANonPositiveInstallmentAmount() {
		Receivable original = overdue("100.00");

		assertThatThrownBy(() -> service.execute(
				command(List.of(original), new Installment(today.plusDays(30), BigDecimal.ZERO))))
				.isInstanceOf(BusinessRuleException.class);

		verify(receivableRepositoryPort, never()).save(any());
		verify(updateCustomerCreditStatusPort, never()).update(any());
	}
}
