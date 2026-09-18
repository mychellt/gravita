package br.gravita.system.domain.model;

import br.gravita.shared.BusinessRuleException;

/**
 * The seven external integrations the platform manages credentials for (doc §11.3).
 * This is a closed set on purpose: {@link #fromCode(String)} rejects anything else so an
 * unknown {@code integrationName} can never be persisted.
 */
public enum IntegrationName {

	SEFAZ("sefaz"),
	RECEITA_FEDERAL("receita-federal"),
	VIACEP_IBGE("viacep-ibge"),
	BANK("bank"),
	WHATSAPP_BUSINESS_API("whatsapp-business-api"),
	ECOMMERCE("ecommerce"),
	ACCOUNTING("accounting");

	private final String code;

	IntegrationName(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static IntegrationName fromCode(String code) {
		if (code != null) {
			for (IntegrationName value : values()) {
				if (value.code.equalsIgnoreCase(code)) {
					return value;
				}
			}
		}
		throw new UnknownIntegrationException(code);
	}
}
