package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.time.Instant;
import java.util.Objects;

/**
 * UC-M2-05: a single CC-e (Carta de Correção Eletrônica) event registered
 * against an {@code AUTHORIZED} {@link NfeDocument} to correct non-tax data
 * without reissuing it. {@code sequenceNumber} is 1-based and assigned by
 * {@link NfeDocument#issueCorrectionLetter} from the number of events already
 * on the document.
 */
public record CorrectionLetter(int sequenceNumber, String text, String protocol, Instant issuedAt) {

	public CorrectionLetter {
		if (sequenceNumber < 1) {
			throw new BusinessRuleException("sequenceNumber must be positive: " + sequenceNumber);
		}
		if (text == null || text.isBlank()) {
			throw new BusinessRuleException("text is required");
		}
		Objects.requireNonNull(protocol, "protocol is required");
		Objects.requireNonNull(issuedAt, "issuedAt is required");
	}
}
