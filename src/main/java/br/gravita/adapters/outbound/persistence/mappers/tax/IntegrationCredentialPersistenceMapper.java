package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.IntegrationCredentialJpaEntity;
import br.gravita.core.domain.system.IntegrationCredential;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface IntegrationCredentialPersistenceMapper {

    @Mapping(target = "encryptedCredentialPayload", source = "encryptedCredentialPayload")
    IntegrationCredentialJpaEntity toEntity(final IntegrationCredential domain, final String encryptedCredentialPayload);

    @Mapping(target = "credentialPayload", source = "decryptedCredentialPayload")
    IntegrationCredential toDomain(final IntegrationCredentialJpaEntity entity, final String decryptedCredentialPayload);
}
