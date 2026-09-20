package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.PaymentTermJpaEntity;
import br.gravita.core.domain.PaymentTermDomain;
import org.mapstruct.Mapper;

@Mapper
public interface PaymentTermPersistenceMapper {

    PaymentTermDomain map(final PaymentTermJpaEntity entity);

    PaymentTermJpaEntity map(final PaymentTermDomain domain);
}
