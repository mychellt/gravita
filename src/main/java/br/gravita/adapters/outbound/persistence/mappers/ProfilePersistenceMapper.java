package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.PermissionJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;
import org.mapstruct.Mapper;

@Mapper
public interface ProfilePersistenceMapper {

    ProfileDomain map(final ProfileJpaEntity entity);

    ProfileJpaEntity map(final ProfileDomain domain);

    PermissionDomain map(final PermissionJpaEntity entity);

    PermissionJpaEntity map(final PermissionDomain domain);
}
