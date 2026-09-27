package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.CorrectionLetter;
import java.time.Instant;

public record CorrectionLetterResponse(int sequenceNumber, String text, String protocol, Instant issuedAt) {

	public static CorrectionLetterResponse from(CorrectionLetter correctionLetter) {
		return new CorrectionLetterResponse(correctionLetter.sequenceNumber(), correctionLetter.text(),
				correctionLetter.protocol(), correctionLetter.issuedAt());
	}
}
