package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeId;
import br.gravita.core.domain.finance.PixChargeStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.ports.inbound.finance.ConfirmPixPaymentCommand;
import br.gravita.core.ports.inbound.finance.ConfirmPixPaymentUseCase;
import br.gravita.core.ports.outbound.persistence.finance.PixChargeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class ConfirmPixPaymentService implements ConfirmPixPaymentUseCase {

	private final PixChargeRepositoryPort pixChargeRepositoryPort;
	private final ReceivableRepositoryPort receivableRepositoryPort;

	public ConfirmPixPaymentService(PixChargeRepositoryPort pixChargeRepositoryPort,
			ReceivableRepositoryPort receivableRepositoryPort) {
		this.pixChargeRepositoryPort = pixChargeRepositoryPort;
		this.receivableRepositoryPort = receivableRepositoryPort;
	}

	/**
	 * The charge and the receivable change together or not at all. A receivable
	 * that is already {@code SETTLED} (e.g. paid by other means first) is left
	 * as is; one that can't be settled at all ({@code CANCELLED},
	 * {@code RENEGOTIATED}) fails the confirmation so the payment is not
	 * silently absorbed.
	 */
	@Override
	@Transactional
	public PixCharge execute(ConfirmPixPaymentCommand command) {
		PixCharge charge = pixChargeRepositoryPort.findById(PixChargeId.of(command.pixChargeId()))
				.orElseThrow(() -> new ResourceNotFoundException("PixCharge not found: " + command.pixChargeId()));
		if (charge.getStatus() == PixChargeStatus.PAID) {
			return charge;
		}

		Receivable receivable = receivableRepositoryPort.findById(charge.getReceivableId()).orElseThrow(
				() -> new ResourceNotFoundException("Receivable not found: " + charge.getReceivableId().value()));
		if (receivable.getStatus() != ReceivableStatus.SETTLED) {
			receivableRepositoryPort.save(receivable.settle());
		}
		return pixChargeRepositoryPort.save(charge.markPaid());
	}
}
