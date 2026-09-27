package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.UUID;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface UserPersistenceMapper {

    @Mapping(target = "passwordHash", source = "passwordHash")
    UserJpaEntity toEntity(final User domain, final String passwordHash);

    default User toDomain(final UserJpaEntity entity) {
        return User.builder()
                .id(UserId.of(entity.getId()))
                .name(entity.getName())
                .email(entity.getEmail())
                .rawPassword(entity.getPasswordHash())
                .profileId(entity.getProfileId())
                .twoFactorEnabled(entity.isTwoFactorEnabled())
                .status(entity.getStatus())
                .build();
    }

    default UUID map(final UserId id) {
        return id == null ? null : id.value();
    }
}
