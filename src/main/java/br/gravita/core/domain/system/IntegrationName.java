package br.gravita.core.domain.system;

public enum IntegrationName {

	SEFAZ("sefaz"),
	RECEITA_FEDERAL("receita-federal"),
	VIACEP_IBGE("viacep-ibge"),
	BANK("bank"),
	WHATSAPP_BUSINESS_API("whatsapp-business-api"),
	ECOMMERCE("ecommerce"),
	ACCOUNTING("accounting");

	private final String code;

	IntegrationName(final String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static IntegrationName fromCode(final String code) {
		if (code != null) {
			for (final IntegrationName value : values()) {
				if (value.code.equalsIgnoreCase(code)) {
					return value;
				}
			}
		}
		throw new UnknownIntegrationException(code);
	}
}
