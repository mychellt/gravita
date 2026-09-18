package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;

public record LookupPersonByDocumentQuery(DocumentDomain document, String cep) {

	public LookupPersonByDocumentQuery {
		boolean hasDocument = document != null;
		boolean hasCep = cep != null && !cep.isBlank();
		if (hasDocument == hasCep) {
			throw new BusinessRuleException("Provide either a CNPJ document or a CEP, not both or neither");
		}
	}

	public static LookupPersonByDocumentQuery byDocument(DocumentDomain document) {
		return new LookupPersonByDocumentQuery(document, null);
	}

	public static LookupPersonByDocumentQuery byCep(String cep) {
		return new LookupPersonByDocumentQuery(null, cep);
	}

	public boolean isDocumentQuery() {
		return document != null;
	}
}
