package br.gravita.core.domain.system;

public enum ApprovalModule {

	PURCHASING("purchasing"),
	SALES("sales"),
	FINANCE("finance");

	private final String code;

	ApprovalModule(final String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static ApprovalModule fromCode(final String code) {
		if (code != null) {
			for (final ApprovalModule value : values()) {
				if (value.code.equalsIgnoreCase(code)) {
					return value;
				}
			}
		}
		throw new UnknownApprovalModuleException(code);
	}
}
