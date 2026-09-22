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

/**
 * A company never holds two active certificates (UC-M1-02): {@link #save} reuses the existing
 * row's id for that {@code companyId} - which the unique {@code company_id} column also
 * enforces at the database level - instead of inserting a second certificate.
 */
@PersistenceAdapter
class CertificateStorageAdapter implements CertificateStoragePort {

	private final DigitalCertificateJpaRepository jpaRepository;
	private final CertificateCipher cipher;
	private final DigitalCertificatePersistenceMapper mapper;

	CertificateStorageAdapter(DigitalCertificateJpaRepository jpaRepository, CertificateCipher cipher,
			DigitalCertificatePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.cipher = cipher;
		this.mapper = mapper;
	}

	@Override
	public DigitalCertificate save(DigitalCertificate certificate) {
		byte[] encryptedPfxPayload = cipher.encrypt(certificate.getPfxPayload());
		String encryptedPassword = cipher.encryptText(certificate.getPassword());

		DigitalCertificateJpaEntity entity = mapper.toEntity(certificate, encryptedPfxPayload, encryptedPassword);
		jpaRepository.findByCompanyId(certificate.getCompanyId().value())
				.ifPresent(existing -> entity.setId(existing.getId()));
		entity.setNew(!jpaRepository.existsById(entity.getId()));

		DigitalCertificateJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved, certificate.getPfxPayload(), certificate.getPassword());
	}

	@Override
	public Optional<DigitalCertificate> findByCompanyId(CompanyId companyId) {
		return jpaRepository.findByCompanyId(companyId.value())
				.map(entity -> mapper.toDomain(entity, cipher.decrypt(entity.getEncryptedPfxPayload()),
						cipher.decryptText(entity.getEncryptedPassword())));
	}
}
