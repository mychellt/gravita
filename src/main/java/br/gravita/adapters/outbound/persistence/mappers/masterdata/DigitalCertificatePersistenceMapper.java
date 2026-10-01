package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DigitalCertificateJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.UUID;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface DigitalCertificatePersistenceMapper {

    @Mapping(target = "id", source = "domain.id")
    @Mapping(target = "companyId", source = "domain.companyId.value")
    @Mapping(target = "type", source = "domain.type")
    @Mapping(target = "expiresAt", source = "domain.expiresAt")
    @Mapping(target = "uploadedAt", source = "domain.uploadedAt")
    DigitalCertificateJpaEntity map(final DigitalCertificate domain, final byte[] encryptedPfxPayload,
            final String encryptedPassword);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "companyId", source = "entity.companyId")
    @Mapping(target = "type", source = "entity.type")
    @Mapping(target = "pfxPayload", source = "decryptedPfxPayload")
    @Mapping(target = "password", source = "decryptedPassword")
    @Mapping(target = "expiresAt", source = "entity.expiresAt")
    @Mapping(target = "uploadedAt", source = "entity.uploadedAt")
    DigitalCertificate map(final DigitalCertificateJpaEntity entity, final byte[] decryptedPfxPayload,
            final String decryptedPassword);

    @Mapping(target = "value", source = "id")
    CompanyId map(final UUID id);
}
