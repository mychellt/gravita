package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.PasswordResetTokenJpaEntity;
import br.gravita.core.domain.system.PasswordResetToken;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PasswordResetTokenPersistenceMapper {

    PasswordResetTokenJpaEntity map(final PasswordResetToken domain);

    // PasswordResetToken has a no-arg constructor but no setters, so it can only be populated through its builder.
    @BeanMapping(builder = @Builder(disableBuilder = false))
    PasswordResetToken map(final PasswordResetTokenJpaEntity entity);
}
