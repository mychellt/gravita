package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.shared.Document;

public record LookupPersonByDocumentQuery(Document document, String cep) {

	public LookupPersonByDocumentQuery {
		final boolean hasDocument = document != null;
		final boolean hasCep = cep != null && !cep.isBlank();
		if (hasDocument == hasCep) {
			throw new BusinessRuleException("Provide either a CNPJ document or a CEP, not both or neither");
		}
	}

	public static LookupPersonByDocumentQuery byDocument(final Document document) {
		return new LookupPersonByDocumentQuery(document, null);
	}

	public static LookupPersonByDocumentQuery byCep(final String cep) {
		return new LookupPersonByDocumentQuery(null, cep);
	}

	public boolean isDocumentQuery() {
		return document != null;
	}
}
