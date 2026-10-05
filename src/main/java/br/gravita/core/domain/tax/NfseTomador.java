package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Recipient of the service (PF or PJ). Like {@link NfeRecipient}, the document keeps its own validated copy of the
 * tomador's data; {@code personRef} is only set for an already registered customer. {@code municipalityIbgeCode} and
 * {@code address} are optional in general, but both are needed ("full address") whenever a tax is withheld at source,
 * and the municipality is also needed when ISS is due to the recipient's municipality.
 */
public record NfseTomador(PersonRef personRef, Document document, String name, String municipalityIbgeCode,
		TomadorAddress address) {

	private static final Pattern IBGE_CODE = Pattern.compile("\\d{7}");

	public NfseTomador {
		Objects.requireNonNull(document, "document is required");
		if (name == null || name.isBlank()) {
			throw new BusinessRuleException("Tomador name is required");
		}
		if (municipalityIbgeCode != null && !IBGE_CODE.matcher(municipalityIbgeCode).matches()) {
			throw new BusinessRuleException("Tomador municipalityIbgeCode must have 7 digits");
		}
	}

	public static NfseTomador of(final PersonRef personRef, final String documentNumber, final PersonType personType, final String name,
			final String municipalityIbgeCode, final TomadorAddress address) {
		if (personType == null) {
			throw new BusinessRuleException("Tomador personType is required");
		}
		final Document document = personType == PersonType.COMPANY ? Document.cnpj(documentNumber)
				: Document.cpf(documentNumber);
		return new NfseTomador(personRef, document, name, municipalityIbgeCode, address);
	}

	public boolean isCompany() {
		return document.personType() == PersonType.COMPANY;
	}

	public boolean hasFullAddress() {
		return address != null && municipalityIbgeCode != null;
	}
}
