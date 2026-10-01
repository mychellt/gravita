package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DigitalCertificateJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.DigitalCertificatePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.DigitalCertificateJpaRepository;
import br.gravita.adapters.outbound.security.CertificateCipher;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateStorageAdapterTest {

	@Mock
	private DigitalCertificateJpaRepository repository;

	@Mock
	private CertificateCipher cipher;

	@Mock
	private DigitalCertificatePersistenceMapper mapper;

	@InjectMocks
	private CertificateStorageAdapter adapter;

	@Test
	@DisplayName("Saves a new certificate persisting only the encrypted payload and password")
	void shouldSaveNewCertificateWithEncryptedPayloadAndPassword() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final DigitalCertificate certificate = buildCertificate(companyId, new byte[] {10, 20, 30}, "top-secret");
		final byte[] encryptedPayload = {99, 98, 97};
		final DigitalCertificateJpaEntity entity = buildEntity(UUID.randomUUID(), companyId);
		final DigitalCertificateJpaEntity saved = buildEntity(entity.getId(), companyId);
		when(cipher.encrypt(certificate.getPfxPayload())).thenReturn(encryptedPayload);
		when(cipher.encryptText("top-secret")).thenReturn("encrypted-password");
		when(mapper.map(certificate, encryptedPayload, "encrypted-password")).thenReturn(entity);
		when(repository.findByCompanyId(companyId.value())).thenReturn(Optional.empty());
		when(repository.existsById(entity.getId())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved, certificate.getPfxPayload(), "top-secret")).thenReturn(certificate);

		final DigitalCertificate result = adapter.save(certificate);

		assertThat(result).isSameAs(certificate);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Replaces the previous certificate when a new one is uploaded for the same company")
	void uploadingNewCertificateReplacesThePreviousOneForTheSameCompany() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final DigitalCertificate certificate = buildCertificate(companyId, new byte[] {2}, "new-password");
		final DigitalCertificateJpaEntity entity = buildEntity(UUID.randomUUID(), companyId);
		final DigitalCertificateJpaEntity existing = buildEntity(UUID.randomUUID(), companyId);
		when(cipher.encrypt(certificate.getPfxPayload())).thenReturn(new byte[] {7});
		when(cipher.encryptText("new-password")).thenReturn("encrypted-new-password");
		when(mapper.map(certificate, new byte[] {7}, "encrypted-new-password")).thenReturn(entity);
		when(repository.findByCompanyId(companyId.value())).thenReturn(Optional.of(existing));
		when(repository.existsById(existing.getId())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity, certificate.getPfxPayload(), "new-password")).thenReturn(certificate);

		adapter.save(certificate);

		assertThat(entity.getId()).isEqualTo(existing.getId());
		assertThat(entity.isNew()).isFalse();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Finds a certificate by company decrypting the payload and password")
	void shouldFindCertificateByCompanyWithDecryptedPayload() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final DigitalCertificate certificate = buildCertificate(companyId, new byte[] {10, 20, 30}, "top-secret");
		final DigitalCertificateJpaEntity entity = buildEntity(UUID.randomUUID(), companyId);
		entity.setEncryptedPfxPayload(new byte[] {99});
		entity.setEncryptedPassword("encrypted-password");
		when(repository.findByCompanyId(companyId.value())).thenReturn(Optional.of(entity));
		when(cipher.decrypt(new byte[] {99})).thenReturn(new byte[] {10, 20, 30});
		when(cipher.decryptText("encrypted-password")).thenReturn("top-secret");
		when(mapper.map(entity, new byte[] {10, 20, 30}, "top-secret")).thenReturn(certificate);

		final Optional<DigitalCertificate> result = adapter.findByCompanyId(companyId);

		assertThat(result).contains(certificate);
		verify(repository).findByCompanyId(companyId.value());
	}

	@Test
	@DisplayName("Returns empty when the company has no certificate")
	void shouldReturnEmptyWhenCompanyHasNoCertificate() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		when(repository.findByCompanyId(companyId.value())).thenReturn(Optional.empty());

		assertThat(adapter.findByCompanyId(companyId)).isEmpty();
	}

	private DigitalCertificateJpaEntity buildEntity(final UUID id, final CompanyId companyId) {
		return DigitalCertificateJpaEntity.builder().id(id).companyId(companyId.value()).build();
	}

	private DigitalCertificate buildCertificate(final CompanyId companyId, final byte[] pfxPayload,
			final String password) {
		return DigitalCertificate.upload(companyId, CertificateType.A1, pfxPayload, password,
				Instant.now().plus(365, ChronoUnit.DAYS));
	}
}
