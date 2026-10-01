package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.PriceTableEntryEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.masterdata.PriceTableJpaEntity;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableEntry;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.ProductOrClassRef;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.UUID;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PriceTablePersistenceMapper {

    PriceTable map(final PriceTableJpaEntity entity);

    @Mapping(target = "id", source = "id.value")
    PriceTableJpaEntity map(final PriceTable domain);

    @Mapping(target = "ref", source = "embeddable")
    PriceTableEntry map(final PriceTableEntryEmbeddable embeddable);

    @Mapping(target = "refType", source = "ref.type")
    @Mapping(target = "referenceId", source = "ref.referenceId")
    PriceTableEntryEmbeddable map(final PriceTableEntry domain);

    @Mapping(target = "type", source = "refType")
    ProductOrClassRef toRef(final PriceTableEntryEmbeddable embeddable);

    @Mapping(target = "value", source = "id")
    PriceTableId map(final UUID id);
}
