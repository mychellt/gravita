package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper
public interface UserPersistenceMapper {

    @Mapping(target = "passwordHash", source = "passwordHash")
    UserJpaEntity toEntity(final User domain, final String passwordHash);

    @Mapping(target = "rawPassword", source = "passwordHash")
    User toDomain(final UserJpaEntity entity);

    default UUID map(final UserId id) {
        return id == null ? null : id.value();
    }

    default UserId map(final UUID id) {
        return id == null ? null : UserId.of(id);
    }
}
