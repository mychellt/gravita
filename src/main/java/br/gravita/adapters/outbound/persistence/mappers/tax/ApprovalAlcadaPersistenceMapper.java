package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ApprovalAlcadaJpaEntity;
import br.gravita.core.domain.system.ApprovalAlcada;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ApprovalAlcadaPersistenceMapper {

    // ApprovalAlcada has a no-arg constructor but no setters, so it can only be populated through its builder.
    @BeanMapping(builder = @Builder(disableBuilder = false))
    ApprovalAlcada map(final ApprovalAlcadaJpaEntity entity);

    ApprovalAlcadaJpaEntity map(final ApprovalAlcada domain);
}
