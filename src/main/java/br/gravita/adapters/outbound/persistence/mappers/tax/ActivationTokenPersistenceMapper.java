package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ActivationTokenJpaEntity;
import br.gravita.core.domain.system.ActivationToken;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ActivationTokenPersistenceMapper {

    ActivationTokenJpaEntity map(final ActivationToken domain);

    // ActivationToken has a no-arg constructor but no setters, so it can only be populated through its builder.
    @BeanMapping(builder = @Builder(disableBuilder = false))
    ActivationToken map(final ActivationTokenJpaEntity entity);
}
