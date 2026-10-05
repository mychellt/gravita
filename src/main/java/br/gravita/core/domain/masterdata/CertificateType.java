package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;

public enum CertificateType {
	A1,
	A3;

	public static CertificateType fromCode(final String code) {
		if (code == null || code.isBlank()) {
			return A1;
		}
		try {
			return CertificateType.valueOf(code.trim().toUpperCase());
		} catch (final IllegalArgumentException e) {
			throw new BusinessRuleException("Unknown certificate type: " + code);
		}
	}
}
