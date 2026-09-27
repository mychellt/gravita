package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.IntegrationCredentialJpaEntity;
import br.gravita.core.domain.system.IntegrationCredential;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface IntegrationCredentialPersistenceMapper {

    @Mapping(target = "encryptedCredentialPayload", source = "encryptedCredentialPayload")
    IntegrationCredentialJpaEntity toEntity(final IntegrationCredential domain, final String encryptedCredentialPayload);

    default IntegrationCredential toDomain(final IntegrationCredentialJpaEntity entity,
            final String decryptedCredentialPayload) {
        return IntegrationCredential.builder()
                .id(entity.getId())
                .integrationName(entity.getIntegrationName())
                .environment(entity.getEnvironment())
                .endpoint(entity.getEndpoint())
                .credentialPayload(decryptedCredentialPayload)
                .rotatedAt(entity.getRotatedAt())
                .build();
    }
}
