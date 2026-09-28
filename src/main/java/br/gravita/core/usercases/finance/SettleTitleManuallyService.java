package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.SettleTitleCommand;
import br.gravita.core.ports.inbound.finance.SettleTitleManuallyUseCase;
import br.gravita.core.ports.outbound.finance.UpdateCustomerCreditStatusPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class SettleTitleManuallyService implements SettleTitleManuallyUseCase {

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;
	private final UpdateCustomerCreditStatusPort updateCustomerCreditStatusPort;

	public SettleTitleManuallyService(ReceivableRepositoryPort receivableRepositoryPort,
			SettlementRepositoryPort settlementRepositoryPort,
			UpdateCustomerCreditStatusPort updateCustomerCreditStatusPort) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
		this.updateCustomerCreditStatusPort = updateCustomerCreditStatusPort;
	}

	/**
	 * What a baixa clears is its principal plus the discount granted; interest,
	 * fine and surcharge are paid on top and do not reduce the title. A
	 * {@code partial} baixa must leave part of the title unpaid and a full one
	 * must clear all of what is left, so the flag can't contradict the amounts;
	 * one that would clear more than what is left is rejected.
	 */
	@Override
	@Transactional
	public Settlement execute(SettleTitleCommand command) {
		Receivable receivable = receivableRepositoryPort.findById(ReceivableId.of(command.receivableId()))
				.orElseThrow(() -> new ResourceNotFoundException("Receivable not found: " + command.receivableId()));
		if (receivable.getStatus() != ReceivableStatus.OPEN
				&& receivable.getStatus() != ReceivableStatus.PARTIALLY_SETTLED) {
			throw new BusinessRuleException(
					"Receivable " + command.receivableId() + " cannot be settled: " + receivable.getStatus());
		}

		Settlement settlement = Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(),
				command.amount(), command.interest(), command.fine(), command.discount(), command.surcharge(),
				Instant.now());
		List<Settlement> previous = settlementRepositoryPort.findByReceivableId(receivable.getId());
		BigDecimal remaining = receivable.remainingBalance(previous);
		int comparison = settlement.creditedAmount().compareTo(remaining);
		if (comparison > 0) {
			throw new BusinessRuleException("Settlement of " + settlement.creditedAmount()
					+ " exceeds the remaining balance of " + remaining);
		}
		if (command.partial() && comparison == 0) {
			throw new BusinessRuleException(
					"Partial settlement must leave part of the remaining balance of " + remaining + " unpaid");
		}
		if (!command.partial() && comparison < 0) {
			throw new BusinessRuleException("Full settlement must cover the remaining balance of " + remaining
					+ ", got " + settlement.creditedAmount());
		}

		Receivable updated = receivable
				.applyCreditedTotal(receivable.getAmount().subtract(remaining).add(settlement.creditedAmount()));
		Settlement saved = settlementRepositoryPort.save(settlement);
		receivableRepositoryPort.save(updated);
		updateCustomerCreditStatusPort.update(receivable.getCustomerId());
		return saved;
	}
}
