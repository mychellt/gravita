package br.gravita.system.domain.model;

/**
 * The three business modules ({@code purchasing}, {@code sales}, {@code finance}) that resolve
 * their approval requirement against a single, centralized {@link ApprovalAlcada} configuration
 * instead of each maintaining its own (doc §11.1, §7, §8.1, §9.2). Closed set on purpose:
 * {@link #fromCode(String)} rejects anything else so an unknown module can never be configured.
 */
public enum ApprovalModule {

	PURCHASING("purchasing"),
	SALES("sales"),
	FINANCE("finance");

	private final String code;

	ApprovalModule(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static ApprovalModule fromCode(String code) {
		if (code != null) {
			for (ApprovalModule value : values()) {
				if (value.code.equalsIgnoreCase(code)) {
					return value;
				}
			}
		}
		throw new UnknownApprovalModuleException(code);
	}
}
