package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.domain.finance.RenegotiationId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.RenegotiateTitleCommand;
import br.gravita.core.ports.inbound.finance.RenegotiateTitleCommand.Installment;
import br.gravita.core.ports.inbound.finance.RenegotiateTitleUseCase;
import br.gravita.core.ports.outbound.finance.UpdateCustomerCreditStatusPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.RenegotiationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RenegotiateTitleService implements RenegotiateTitleUseCase {

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;
	private final RenegotiationRepositoryPort renegotiationRepositoryPort;
	private final UpdateCustomerCreditStatusPort updateCustomerCreditStatusPort;

	public RenegotiateTitleService(ReceivableRepositoryPort receivableRepositoryPort,
			SettlementRepositoryPort settlementRepositoryPort, RenegotiationRepositoryPort renegotiationRepositoryPort,
			UpdateCustomerCreditStatusPort updateCustomerCreditStatusPort) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
		this.renegotiationRepositoryPort = renegotiationRepositoryPort;
		this.updateCustomerCreditStatusPort = updateCustomerCreditStatusPort;
	}

	/**
	 * One transaction: the originals, the new titles, the renegotiation and the
	 * customer's credit status change together or not at all.
	 */
	@Override
	@Transactional
	public Renegotiation execute(RenegotiateTitleCommand command) {
		if (command.originalReceivableIds().isEmpty()) {
			throw new BusinessRuleException("At least one receivable to renegotiate is required");
		}
		if (command.newInstallmentPlan().isEmpty()) {
			throw new BusinessRuleException("At least one installment is required");
		}
		if (new HashSet<>(command.originalReceivableIds()).size() != command.originalReceivableIds().size()) {
			throw new BusinessRuleException("A receivable can only be renegotiated once per renegotiation");
		}

		LocalDate today = LocalDate.now();
		List<Receivable> originals = command.originalReceivableIds().stream().map(this::load).toList();
		UUID customerId = originals.get(0).getCustomerId();
		if (originals.stream().anyMatch(original -> !original.getCustomerId().equals(customerId))) {
			throw new BusinessRuleException("All renegotiated receivables must belong to the same customer");
		}
		List<Receivable> renegotiated = originals.stream().map(original -> original.renegotiate(today)).toList();

		int total = command.newInstallmentPlan().size();
		List<Receivable> created = new ArrayList<>(total);
		for (int i = 0; i < total; i++) {
			Installment installment = command.newInstallmentPlan().get(i);
			if (installment.dueDate().isBefore(today)) {
				throw new BusinessRuleException("Installment " + (i + 1) + " is already due: " + installment.dueDate());
			}
			created.add(Receivable.createFromRenegotiation(ReceivableId.of(UUID.randomUUID()), customerId,
					installment.amount(), installment.dueDate(), i + 1, total));
		}

		Renegotiation renegotiation = Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), customerId,
				renegotiated.stream().map(Receivable::getId).toList(),
				created.stream().map(Receivable::getId).toList(), Instant.now());

		renegotiated.forEach(receivableRepositoryPort::save);
		created.forEach(receivableRepositoryPort::save);
		Renegotiation saved = renegotiationRepositoryPort.save(renegotiation);

		refreshCreditStatus(customerId, today);
		return saved;
	}

	private Receivable load(UUID receivableId) {
		return receivableRepositoryPort.findById(ReceivableId.of(receivableId))
				.orElseThrow(() -> new ResourceNotFoundException("Receivable not found: " + receivableId));
	}

	/**
	 * The customer's position after the change: what is still owed on the
	 * outstanding titles, and delinquent while any of them is overdue.
	 */
	private void refreshCreditStatus(UUID customerId, LocalDate today) {
		List<Receivable> outstanding = receivableRepositoryPort.findOutstandingByCustomerId(customerId);
		BigDecimal balance = outstanding.stream().map(this::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
		CustomerStatus status = outstanding.stream().anyMatch(receivable -> receivable.isOverdue(today))
				? CustomerStatus.DELINQUENT
				: CustomerStatus.REGULAR;
		updateCustomerCreditStatusPort.update(customerId, balance, status);
	}

	private BigDecimal remaining(Receivable receivable) {
		BigDecimal credited = settlementRepositoryPort.findByReceivableId(receivable.getId()).stream()
				.map(Settlement::creditedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return receivable.getAmount().subtract(credited).max(BigDecimal.ZERO);
	}
}
