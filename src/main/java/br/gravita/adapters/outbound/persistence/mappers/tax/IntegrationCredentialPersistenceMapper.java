package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.IntegrationCredentialJpaEntity;
import br.gravita.core.domain.system.IntegrationCredential;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface IntegrationCredentialPersistenceMapper {

    @Mapping(target = "encryptedCredentialPayload", source = "encryptedCredentialPayload")
    IntegrationCredentialJpaEntity map(final IntegrationCredential domain, final String encryptedCredentialPayload);

    // IntegrationCredential has a no-arg constructor but no setters, so it can only be populated through its builder.
    @BeanMapping(builder = @Builder(disableBuilder = false))
    @Mapping(target = "credentialPayload", source = "decryptedCredentialPayload")
    IntegrationCredential map(final IntegrationCredentialJpaEntity entity, final String decryptedCredentialPayload);
}
