package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DigitalCertificateJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.DigitalCertificatePersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.DigitalCertificateJpaRepository;
import br.gravita.adapters.outbound.security.CertificateCipher;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({CertificateStorageAdapter.class, CertificateCipher.class, DigitalCertificatePersistenceMapperImpl.class})
class CertificateStorageAdapterTest {

	@Autowired
	private CertificateStorageAdapter repositoryAdapter;

	@Autowired
	private DigitalCertificateJpaRepository jpaRepository;

	private DigitalCertificate certificateFor(CompanyId companyId, byte[] pfxPayload, String password) {
		return DigitalCertificate.upload(companyId, CertificateType.A1, pfxPayload, password,
				Instant.now().plus(365, ChronoUnit.DAYS));
	}

	@Test
	@DisplayName("Saves a certificate and retrieves it with the payload decrypted")
	void shouldSaveAndRetrieveCertificateWithDecryptedPayload() {
		CompanyId companyId = CompanyId.of(UUID.randomUUID());
		DigitalCertificate certificate = certificateFor(companyId, new byte[] {10, 20, 30}, "top-secret");

		repositoryAdapter.save(certificate);

		Optional<DigitalCertificate> found = repositoryAdapter.findByCompanyId(companyId);
		assertThat(found).isPresent();
		assertThat(found.get().getPfxPayload()).isEqualTo(new byte[] {10, 20, 30});
		assertThat(found.get().getPassword()).isEqualTo("top-secret");
		assertThat(found.get().getExpiresAt()).isEqualTo(certificate.getExpiresAt());
	}

	@Test
	@DisplayName("Never persists the certificate payload or password in plaintext")
	void shouldNeverPersistThePlaintextPayloadOrPassword() {
		CompanyId companyId = CompanyId.of(UUID.randomUUID());
		byte[] plainPayload = "plain-pfx-bytes".getBytes();
		DigitalCertificate certificate = certificateFor(companyId, plainPayload, "plain-password-value");

		repositoryAdapter.save(certificate);

		DigitalCertificateJpaEntity stored = jpaRepository.findAll().get(0);
		assertThat(new String(stored.getEncryptedPfxPayload())).doesNotContain("plain-pfx-bytes");
		assertThat(stored.getEncryptedPassword()).doesNotContain("plain-password-value");
	}

	@Test
	@DisplayName("Replaces the previous certificate when a new one is uploaded for the same company")
	void uploadingNewCertificateReplacesThePreviousOneForTheSameCompany() {
		CompanyId companyId = CompanyId.of(UUID.randomUUID());
		repositoryAdapter.save(certificateFor(companyId, new byte[] {1}, "old-password"));

		repositoryAdapter.save(certificateFor(companyId, new byte[] {2}, "new-password"));

		assertThat(jpaRepository.findAll()).hasSize(1);
		Optional<DigitalCertificate> found = repositoryAdapter.findByCompanyId(companyId);
		assertThat(found).isPresent();
		assertThat(found.get().getPassword()).isEqualTo("new-password");
	}

	@Test
	@DisplayName("Returns empty when the company has no certificate")
	void shouldReturnEmptyWhenCompanyHasNoCertificate() {
		assertThat(repositoryAdapter.findByCompanyId(CompanyId.of(UUID.randomUUID()))).isEmpty();
	}
}
