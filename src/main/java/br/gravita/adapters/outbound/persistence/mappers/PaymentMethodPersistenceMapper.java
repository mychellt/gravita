package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.PaymentMethodJpaEntity;
import br.gravita.core.domain.PaymentMethodDomain;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PaymentMethodPersistenceMapper {

    PaymentMethodDomain map(final PaymentMethodJpaEntity entity);

    PaymentMethodJpaEntity map(final PaymentMethodDomain domain);
}
