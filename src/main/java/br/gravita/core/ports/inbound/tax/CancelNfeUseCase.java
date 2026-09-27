package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.NfeDocument;

/**
 * UC-M2-04: cancels an {@code AUTHORIZED} {@link NfeDocument} within its
 * legal cancellation window, with a mandatory justification. The document is
 * never physically deleted - it moves to {@code CANCELLED} and stays
 * queryable.
 */
public interface CancelNfeUseCase {

	NfeDocument execute(CancelNfeCommand command);
}
