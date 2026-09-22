package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DigitalCertificateJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * {@code DigitalCertificate} exposes no setters (immutable outside its validating
 * constructor), so disableBuilder leaves MapStruct with no write accessor to target - both
 * directions are built by hand, the same approach used by {@code IntegrationCredentialPersistenceMapper}.
 */
@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface DigitalCertificatePersistenceMapper {

	default DigitalCertificateJpaEntity toEntity(final DigitalCertificate domain, final byte[] encryptedPfxPayload,
			final String encryptedPassword) {
		return DigitalCertificateJpaEntity.builder()
				.id(domain.getId())
				.companyId(domain.getCompanyId().value())
				.type(domain.getType())
				.encryptedPfxPayload(encryptedPfxPayload)
				.encryptedPassword(encryptedPassword)
				.expiresAt(domain.getExpiresAt())
				.uploadedAt(domain.getUploadedAt())
				.build();
	}

	default DigitalCertificate toDomain(final DigitalCertificateJpaEntity entity, final byte[] decryptedPfxPayload,
			final String decryptedPassword) {
		return DigitalCertificate.of(entity.getId(), CompanyId.of(entity.getCompanyId()), entity.getType(),
				decryptedPfxPayload, decryptedPassword, entity.getExpiresAt(), entity.getUploadedAt());
	}
}
