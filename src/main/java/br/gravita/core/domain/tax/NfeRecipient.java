package br.gravita.core.domain.tax;

import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The party an outbound NFe is issued to (module spec §3.1): either a
 * registered {@code Customer} ({@code customerId} set) or ad-hoc PF/PJ data
 * typed in for a walk-in/one-off recipient. {@code document}'s check-digit
 * is already validated by {@link Document#cpf}/{@link Document#cnpj} at
 * construction (AC3, first half); this record adds the IE-taxpayer half:
 * a recipient declared as {@link IeIndicator#TAXPAYER} must carry an IE.
 */
public record NfeRecipient(UUID customerId, Document document, String name, IeIndicator ieIndicator, String ie,
		String state) {

	private static final Pattern IE_DIGITS = Pattern.compile("\\d{2,14}");

	public NfeRecipient {
		if (document == null) {
			throw new BusinessRuleException("Recipient document is required");
		}
		if (name == null || name.isBlank()) {
			throw new BusinessRuleException("Recipient name is required");
		}
		if (ieIndicator == null) {
			throw new BusinessRuleException("Recipient IE indicator is required");
		}
		if (state == null || state.isBlank()) {
			throw new BusinessRuleException("Recipient state (UF) is required");
		}
		if (ieIndicator == IeIndicator.TAXPAYER && (ie == null || !IE_DIGITS.matcher(ie.trim()).matches())) {
			throw new BusinessRuleException("A taxpayer recipient requires a valid IE: " + ie);
		}
	}
}
