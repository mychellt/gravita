package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.ChartOfAccountsJpaEntity;
import br.gravita.core.domain.ChartOfAccountsDomain;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ChartOfAccountsPersistenceMapper {

    ChartOfAccountsDomain map(final ChartOfAccountsJpaEntity entity);

    ChartOfAccountsJpaEntity map(final ChartOfAccountsDomain domain);
}
