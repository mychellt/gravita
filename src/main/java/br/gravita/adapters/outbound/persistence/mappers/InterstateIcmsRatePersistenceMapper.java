package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.InterstateIcmsRateJpaEntity;
import br.gravita.core.domain.InterstateIcmsRateDomain;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface InterstateIcmsRatePersistenceMapper {

    InterstateIcmsRateDomain map(final InterstateIcmsRateJpaEntity entity);

    InterstateIcmsRateJpaEntity map(final InterstateIcmsRateDomain domain);
}
