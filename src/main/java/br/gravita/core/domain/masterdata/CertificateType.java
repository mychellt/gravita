package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;

/**
 * A1 (software, exportable) vs A3 (hardware token) ICP-Brasil certificates. Only A1 is
 * accepted for upload in the MVP (doc §2.1) - A3 keys never leave their token, so they
 * cannot be delivered as a {@code .pfx} payload in the first place.
 */
public enum CertificateType {
	A1,
	A3;

	public static CertificateType fromCode(String code) {
		if (code == null || code.isBlank()) {
			return A1;
		}
		try {
			return CertificateType.valueOf(code.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BusinessRuleException("Unknown certificate type: " + code);
		}
	}
}
