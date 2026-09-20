package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.ChartOfAccountsJpaEntity;
import br.gravita.core.domain.ChartOfAccountsDomain;
import org.mapstruct.Mapper;

@Mapper
public interface ChartOfAccountsPersistenceMapper {

    ChartOfAccountsDomain map(final ChartOfAccountsJpaEntity entity);

    ChartOfAccountsJpaEntity map(final ChartOfAccountsDomain domain);
}
