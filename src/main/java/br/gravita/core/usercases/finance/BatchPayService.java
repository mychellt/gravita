package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.CnabRemittance;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.BatchPayCommand;
import br.gravita.core.ports.inbound.finance.BatchPayUseCase;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.IssuedRemittance;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.RemittanceItem;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.RemittanceRequest;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class BatchPayService implements BatchPayUseCase {

	private final PayableRepositoryPort payableRepositoryPort;
	private final BankIntegrationPort bankIntegrationPort;

	@Override
	public CnabRemittance execute(BatchPayCommand command) {
		List<PayableId> ids = requireDistinct(command.payableIds());
		List<Payable> payables = loadInRequestedOrder(ids);
		// Checked before the bank is contacted so a rejected batch never sends a remittance.
		requireAllApproved(payables);

		BigDecimal total = payables.stream().map(Payable::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		IssuedRemittance issued = bankIntegrationPort.sendRemittance(new RemittanceRequest(command.bankIntegration(),
				payables.stream().map(payable -> new RemittanceItem(payable.getId().value(), payable.getSupplierId(),
						payable.getAmount(), payable.getDueDate())).toList()));

		return new CnabRemittance(issued.reference(), command.bankIntegration(), ids, total, issued.fileContent());
	}

	/** A payable listed twice would be paid twice in the same remittance. */
	private static List<PayableId> requireDistinct(List<UUID> payableIds) {
		if (payableIds.isEmpty()) {
			throw new BusinessRuleException("payableIds is required: select at least one payable");
		}
		Set<UUID> seen = new HashSet<>();
		for (UUID id : payableIds) {
			if (!seen.add(id)) {
				throw new BusinessRuleException("payableIds repeats payable " + id);
			}
		}
		return payableIds.stream().map(PayableId::of).toList();
	}

	private List<Payable> loadInRequestedOrder(List<PayableId> ids) {
		Map<PayableId, Payable> found = payableRepositoryPort.findByIds(ids).stream()
				.collect(Collectors.toMap(Payable::getId, Function.identity()));
		List<PayableId> missing = ids.stream().filter(id -> !found.containsKey(id)).toList();
		if (!missing.isEmpty()) {
			throw new ResourceNotFoundException("Payable not found: " + missing.stream()
					.map(id -> id.value().toString()).collect(Collectors.joining(", ")));
		}
		return ids.stream().map(found::get).toList();
	}

	private static void requireAllApproved(List<Payable> payables) {
		List<Payable> notApproved = payables.stream()
				.filter(payable -> payable.getStatus() != PayableStatus.APPROVED).toList();
		if (!notApproved.isEmpty()) {
			throw new BusinessRuleException("Only APPROVED payables can be paid in a batch: " + notApproved.stream()
					.map(payable -> payable.getId().value() + " is " + payable.getStatus())
					.collect(Collectors.joining(", ")));
		}
	}
}
