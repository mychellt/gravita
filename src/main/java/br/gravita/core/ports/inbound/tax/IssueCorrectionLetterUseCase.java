package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.CorrectionLetter;

/**
 * UC-M2-05: registers a CC-e (Carta de Correção Eletrônica) event correcting
 * non-tax data on an already-{@code AUTHORIZED} NFe, without reissuing it.
 */
public interface IssueCorrectionLetterUseCase {

	CorrectionLetter execute(IssueCorrectionLetterCommand command);
}
