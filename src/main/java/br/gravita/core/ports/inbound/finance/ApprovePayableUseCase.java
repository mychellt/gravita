package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Payable;

/**
 * UC-M8-12: approves an {@code OPEN} payable against the finance approval alçada
 * (M10) before it becomes eligible for payment, recording who approved it. The
 * approver acts remotely, via app or e-mail.
 */
public interface ApprovePayableUseCase {

	Payable execute(ApprovePayableCommand command);
}
