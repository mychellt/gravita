package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DigitalCertificateJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.DigitalCertificatePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.DigitalCertificateJpaRepository;
import br.gravita.adapters.outbound.security.CertificateCipher;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import br.gravita.core.ports.outbound.persistence.CertificateStoragePort;

import java.util.Optional;

@PersistenceAdapter
class CertificateStorageAdapter implements CertificateStoragePort {

	private final DigitalCertificateJpaRepository jpaRepository;
	private final CertificateCipher cipher;
	private final DigitalCertificatePersistenceMapper mapper;

	CertificateStorageAdapter(final DigitalCertificateJpaRepository jpaRepository, final CertificateCipher cipher,
			final DigitalCertificatePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.cipher = cipher;
		this.mapper = mapper;
	}

	@Override
	public DigitalCertificate save(final DigitalCertificate certificate) {
		final byte[] encryptedPfxPayload = cipher.encrypt(certificate.getPfxPayload());
		final String encryptedPassword = cipher.encryptText(certificate.getPassword());

		final DigitalCertificateJpaEntity entity = mapper.map(certificate, encryptedPfxPayload, encryptedPassword);
		jpaRepository.findByCompanyId(certificate.getCompanyId().value())
				.ifPresent(existing -> entity.setId(existing.getId()));
		entity.setNew(!jpaRepository.existsById(entity.getId()));

		final DigitalCertificateJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved, certificate.getPfxPayload(), certificate.getPassword());
	}

	@Override
	public Optional<DigitalCertificate> findByCompanyId(final CompanyId companyId) {
		return jpaRepository.findByCompanyId(companyId.value())
				.map(entity -> mapper.map(entity, cipher.decrypt(entity.getEncryptedPfxPayload()),
						cipher.decryptText(entity.getEncryptedPassword())));
	}
}
