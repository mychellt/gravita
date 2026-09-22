package br.gravita.masterdata.domain.model;

import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import br.gravita.core.domain.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DigitalCertificateTest {

	private static final CompanyId COMPANY_ID = CompanyId.of(UUID.randomUUID());
	private static final byte[] PFX_PAYLOAD = {1, 2, 3, 4};

	@Test
	void shouldUploadA1Certificate() {
		Instant expiresAt = Instant.now().plus(365, ChronoUnit.DAYS);

		DigitalCertificate certificate = DigitalCertificate.upload(COMPANY_ID, CertificateType.A1, PFX_PAYLOAD,
				"secret", expiresAt);

		assertThat(certificate.getId()).isNotNull();
		assertThat(certificate.getCompanyId()).isEqualTo(COMPANY_ID);
		assertThat(certificate.getType()).isEqualTo(CertificateType.A1);
		assertThat(certificate.getPfxPayload()).isEqualTo(PFX_PAYLOAD);
		assertThat(certificate.getPassword()).isEqualTo("secret");
		assertThat(certificate.getExpiresAt()).isEqualTo(expiresAt);
	}

	@Test
	void shouldRejectA3Certificate() {
		assertThatThrownBy(() -> DigitalCertificate.upload(COMPANY_ID, CertificateType.A3, PFX_PAYLOAD, "secret",
				Instant.now().plus(1, ChronoUnit.DAYS)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("A1");
	}

	@Test
	void shouldRejectEmptyPayload() {
		assertThatThrownBy(() -> DigitalCertificate.upload(COMPANY_ID, CertificateType.A1, new byte[0], "secret",
				Instant.now().plus(1, ChronoUnit.DAYS)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Certificate file");
	}

	@Test
	void shouldRejectBlankPassword() {
		assertThatThrownBy(() -> DigitalCertificate.upload(COMPANY_ID, CertificateType.A1, PFX_PAYLOAD, " ",
				Instant.now().plus(1, ChronoUnit.DAYS)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("password");
	}

	@Test
	void shouldRejectNullExpiryDate() {
		assertThatThrownBy(() -> DigitalCertificate.upload(COMPANY_ID, CertificateType.A1, PFX_PAYLOAD, "secret", null))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("expiry");
	}

	@Test
	void shouldReportExpiredCertificate() {
		DigitalCertificate certificate = DigitalCertificate.upload(COMPANY_ID, CertificateType.A1, PFX_PAYLOAD,
				"secret", Instant.now().minus(1, ChronoUnit.DAYS));

		assertThat(certificate.isExpired(Instant.now())).isTrue();
	}

	@Test
	void shouldReportActiveCertificateAsNotExpired() {
		DigitalCertificate certificate = DigitalCertificate.upload(COMPANY_ID, CertificateType.A1, PFX_PAYLOAD,
				"secret", Instant.now().plus(1, ChronoUnit.DAYS));

		assertThat(certificate.isExpired(Instant.now())).isFalse();
	}

	@Test
	void shouldDefensivelyCopyPfxPayload() {
		byte[] payload = {5, 6, 7};
		DigitalCertificate certificate = DigitalCertificate.upload(COMPANY_ID, CertificateType.A1, payload, "secret",
				Instant.now().plus(1, ChronoUnit.DAYS));

		certificate.getPfxPayload()[0] = 9;

		assertThat(certificate.getPfxPayload()).isEqualTo(new byte[] {5, 6, 7});
	}
}
