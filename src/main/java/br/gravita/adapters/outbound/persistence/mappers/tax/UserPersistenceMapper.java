package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.UUID;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface UserPersistenceMapper {

    @Mapping(target = "id", source = "domain.id.value")
    @Mapping(target = "passwordHash", source = "passwordHash")
    UserJpaEntity map(final User domain, final String passwordHash);

    // User has a no-arg constructor but no setters, so it can only be populated through its builder.
    @BeanMapping(builder = @Builder(disableBuilder = false))
    @Mapping(target = "rawPassword", source = "passwordHash")
    User map(final UserJpaEntity entity);

    @Mapping(target = "value", source = "id")
    UserId mapUserId(final UUID id);
}
