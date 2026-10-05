package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.Getter;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Getter
public final class DigitalCertificate {

	private final UUID id;
	private final CompanyId companyId;
	private final CertificateType type;
	private final byte[] pfxPayload;
	private final String password;
	private final Instant expiresAt;
	private final Instant uploadedAt;

	public DigitalCertificate(final UUID id, final CompanyId companyId, final CertificateType type, final byte[] pfxPayload, final String password,
			final Instant expiresAt, final Instant uploadedAt) {
		this.id = id;
		this.companyId = requireCompanyId(companyId);
		this.type = requireA1(type);
		this.pfxPayload = requirePfxPayload(pfxPayload);
		this.password = requirePassword(password);
		this.expiresAt = requireExpiresAt(expiresAt);
		this.uploadedAt = uploadedAt;
	}

	public static DigitalCertificate upload(final CompanyId companyId, final CertificateType type, final byte[] pfxPayload,
			final String password, final Instant expiresAt) {
		return new DigitalCertificate(UUID.randomUUID(), companyId, type, pfxPayload, password, expiresAt,
				Instant.now());
	}

	public static DigitalCertificate of(final UUID id, final CompanyId companyId, final CertificateType type, final byte[] pfxPayload,
			final String password, final Instant expiresAt, final Instant uploadedAt) {
		return new DigitalCertificate(id, companyId, type, pfxPayload, password, expiresAt, uploadedAt);
	}

	public boolean isExpired(final Instant asOf) {
		return expiresAt.isBefore(asOf);
	}

	public byte[] getPfxPayload() {
		return Arrays.copyOf(pfxPayload, pfxPayload.length);
	}

	private static CompanyId requireCompanyId(final CompanyId companyId) {
		if (companyId == null) {
			throw new BusinessRuleException("Company id is required");
		}
		return companyId;
	}

	private static CertificateType requireA1(final CertificateType type) {
		if (type != CertificateType.A1) {
			throw new BusinessRuleException(
					"Only A1 certificates are accepted in this release; A3 (token) support is not available yet");
		}
		return type;
	}

	private static byte[] requirePfxPayload(final byte[] pfxPayload) {
		if (pfxPayload == null || pfxPayload.length == 0) {
			throw new BusinessRuleException("Certificate file is required");
		}
		return Arrays.copyOf(pfxPayload, pfxPayload.length);
	}

	private static String requirePassword(final String password) {
		if (password == null || password.isBlank()) {
			throw new BusinessRuleException("Certificate password is required");
		}
		return password;
	}

	private static Instant requireExpiresAt(final Instant expiresAt) {
		if (expiresAt == null) {
			throw new BusinessRuleException("Certificate expiry date is required");
		}
		return expiresAt;
	}
}
