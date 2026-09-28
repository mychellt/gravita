package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserNotFoundException;
import br.gravita.core.ports.inbound.finance.ApprovePayableCommand;
import br.gravita.core.ports.inbound.finance.ApprovePayableUseCase;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@UseCase
public class ApprovePayableService implements ApprovePayableUseCase {

	private final PayableRepositoryPort payableRepositoryPort;
	private final ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;
	private final UserRepositoryPort userRepositoryPort;

	@Override
	@Transactional
	public Payable execute(ApprovePayableCommand command) {
		Payable payable = payableRepositoryPort.findById(PayableId.of(command.payableId()))
				.orElseThrow(() -> new ResourceNotFoundException("Payable not found: " + command.payableId()));

		Payable approved = payable.approve(command.approvedBy());

		approvalAlcadaRepositoryPort.findByModule(ApprovalModule.FINANCE)
				.filter(alcada -> exceedsAlcada(payable, alcada))
				.ifPresent(alcada -> requireElevatedApprover(payable, command, alcada));

		return payableRepositoryPort.save(approved);
	}

	private boolean exceedsAlcada(Payable payable, ApprovalAlcada alcada) {
		return alcada.getThresholdValue() != null && payable.getAmount().compareTo(alcada.getThresholdValue()) >= 0;
	}

	private void requireElevatedApprover(Payable payable, ApprovePayableCommand command, ApprovalAlcada alcada) {
		User approver = userRepositoryPort.findById(UserId.of(command.approvedBy()))
				.orElseThrow(() -> new UserNotFoundException(command.approvedBy()));
		if (!approver.getProfileId().equals(alcada.getApproverProfileId())) {
			throw new BusinessRuleException("Payable " + payable.getId().value() + " of " + payable.getAmount()
					+ " exceeds the finance alcada and requires an approver with the elevated profile");
		}
	}
}
