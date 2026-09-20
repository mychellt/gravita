package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ApprovalAlcadaJpaEntity;
import br.gravita.core.domain.system.ApprovalAlcada;
import org.mapstruct.Mapper;

@Mapper
public interface ApprovalAlcadaPersistenceMapper {

    ApprovalAlcada map(final ApprovalAlcadaJpaEntity entity);

    ApprovalAlcadaJpaEntity map(final ApprovalAlcada domain);
}
