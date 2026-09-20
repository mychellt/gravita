package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.InterstateIcmsRateJpaEntity;
import br.gravita.core.domain.InterstateIcmsRateDomain;
import org.mapstruct.Mapper;

@Mapper
public interface InterstateIcmsRatePersistenceMapper {

    InterstateIcmsRateDomain map(final InterstateIcmsRateJpaEntity entity);

    InterstateIcmsRateJpaEntity map(final InterstateIcmsRateDomain domain);
}
