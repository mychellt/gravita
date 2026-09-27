package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * NFe recipient (doc §3.1: "busca por CPF/CNPJ/nome; validação de IE e
 * indicador de contribuinte"). {@code personRef} is set when the recipient is
 * an existing registered customer and left {@code null} for a one-off,
 * ad-hoc PF/PJ recipient (UC-M2-01 command spec) — either way, the fiscal
 * document keeps its own validated copy of the document/name/IE rather than
 * depending on a live lookup at transmission time.
 */
public record NfeRecipient(PersonRef personRef, Document document, String name, String stateRegistration,
		String state) {

	private static final Pattern IE_DIGITS = Pattern.compile("\\d{2,14}");
	private static final String ISENTO = "ISENTO";

	public NfeRecipient {
		Objects.requireNonNull(document, "document is required");
		if (name == null || name.isBlank()) {
			throw new BusinessRuleException("Recipient name is required");
		}
		if (state == null || state.isBlank()) {
			// Needed to tell an interstate operation from an in-state one for ICMS (CalculateTaxUseCase).
			throw new BusinessRuleException("Recipient state (UF) is required");
		}
		stateRegistration = validateStateRegistration(document.personType(), stateRegistration);
	}

	/**
	 * AC3: the recipient's CPF/CNPJ must pass check-digit validation, and a
	 * COMPANY (CNPJ) recipient must carry a valid IE or an explicit exemption
	 * ("ISENTO") before the document can be created.
	 */
	public static NfeRecipient of(PersonRef personRef, String documentNumber, PersonType personType, String name,
			String stateRegistration, String state) {
		Document document = personType == PersonType.COMPANY ? Document.cnpj(documentNumber)
				: Document.cpf(documentNumber);
		return new NfeRecipient(personRef, document, name, stateRegistration, state.trim().toUpperCase());
	}

	private static String validateStateRegistration(PersonType personType, String stateRegistration) {
		if (personType != PersonType.COMPANY) {
			// Individuals are typically final consumers with no state registration.
			return stateRegistration == null ? null : stateRegistration.trim();
		}
		if (stateRegistration == null || stateRegistration.isBlank()) {
			throw new BusinessRuleException("Recipient IE is required for a company (CNPJ) recipient");
		}
		String trimmed = stateRegistration.trim();
		if (!ISENTO.equalsIgnoreCase(trimmed) && !IE_DIGITS.matcher(trimmed).matches()) {
			throw new BusinessRuleException("Invalid recipient IE: " + stateRegistration);
		}
		return trimmed;
	}
}
