package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.Getter;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * A company's A1 certificate (UC-M1-02). The {@code pfxPayload}/{@code password} fields are
 * plaintext from the domain's point of view - encrypting them at rest (doc §11.4) is a
 * persistence-adapter concern, the same split used by {@code IntegrationCredential}. Expiry is
 * exposed via {@link #isExpired(Instant)} so fiscal-issuance use cases (M2/M3/M4) can enforce
 * the "no expired/absent certificate" invariant at issuance time.
 */
@Getter
public final class DigitalCertificate {

	private final UUID id;
	private final CompanyId companyId;
	private final CertificateType type;
	private final byte[] pfxPayload;
	private final String password;
	private final Instant expiresAt;
	private final Instant uploadedAt;

	private DigitalCertificate(UUID id, CompanyId companyId, CertificateType type, byte[] pfxPayload, String password,
			Instant expiresAt, Instant uploadedAt) {
		this.id = id;
		this.companyId = requireCompanyId(companyId);
		this.type = requireA1(type);
		this.pfxPayload = requirePfxPayload(pfxPayload);
		this.password = requirePassword(password);
		this.expiresAt = requireExpiresAt(expiresAt);
		this.uploadedAt = uploadedAt;
	}

	public static DigitalCertificate upload(CompanyId companyId, CertificateType type, byte[] pfxPayload,
			String password, Instant expiresAt) {
		return new DigitalCertificate(UUID.randomUUID(), companyId, type, pfxPayload, password, expiresAt,
				Instant.now());
	}

	public static DigitalCertificate of(UUID id, CompanyId companyId, CertificateType type, byte[] pfxPayload,
			String password, Instant expiresAt, Instant uploadedAt) {
		return new DigitalCertificate(id, companyId, type, pfxPayload, password, expiresAt, uploadedAt);
	}

	public boolean isExpired(Instant asOf) {
		return expiresAt.isBefore(asOf);
	}

	public byte[] getPfxPayload() {
		return Arrays.copyOf(pfxPayload, pfxPayload.length);
	}

	private static CompanyId requireCompanyId(CompanyId companyId) {
		if (companyId == null) {
			throw new BusinessRuleException("Company id is required");
		}
		return companyId;
	}

	private static CertificateType requireA1(CertificateType type) {
		if (type != CertificateType.A1) {
			throw new BusinessRuleException(
					"Only A1 certificates are accepted in this release; A3 (token) support is not available yet");
		}
		return type;
	}

	private static byte[] requirePfxPayload(byte[] pfxPayload) {
		if (pfxPayload == null || pfxPayload.length == 0) {
			throw new BusinessRuleException("Certificate file is required");
		}
		return Arrays.copyOf(pfxPayload, pfxPayload.length);
	}

	private static String requirePassword(String password) {
		if (password == null || password.isBlank()) {
			throw new BusinessRuleException("Certificate password is required");
		}
		return password;
	}

	private static Instant requireExpiresAt(Instant expiresAt) {
		if (expiresAt == null) {
			throw new BusinessRuleException("Certificate expiry date is required");
		}
		return expiresAt;
	}
}
