package br.gravita.system.domain.model;

import br.gravita.shared.BusinessRuleException;

public class UnknownIntegrationException extends BusinessRuleException {

	public UnknownIntegrationException(String integrationName) {
		super("Unknown integration '" + integrationName + "'. Valid integrations: sefaz, receita-federal, "
				+ "viacep-ibge, bank, whatsapp-business-api, ecommerce, accounting.");
	}
}
