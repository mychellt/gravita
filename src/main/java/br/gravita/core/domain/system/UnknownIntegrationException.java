package br.gravita.core.domain.system;

import br.gravita.core.domain.shared.BusinessRuleException;

public class UnknownIntegrationException extends BusinessRuleException {

	public UnknownIntegrationException(final String integrationName) {
		super("Unknown integration '" + integrationName + "'. Valid integrations: sefaz, receita-federal, "
				+ "viacep-ibge, bank, whatsapp-business-api, ecommerce, accounting.");
	}
}
