package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.PriceTableEntryEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.masterdata.PriceTableJpaEntity;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableEntry;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.ProductOrClassRef;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface PriceTablePersistenceMapper {

    default PriceTable toDomain(final PriceTableJpaEntity entity) {
        return PriceTable.of(
                PriceTableId.of(entity.getId()),
                entity.getFormation(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getMaxDiscountPercent(),
                entity.getMaxDiscountBehavior(),
                toEntries(entity.getEntries()));
    }

    default PriceTableJpaEntity toEntity(final PriceTable domain) {
        return PriceTableJpaEntity.builder()
                .id(domain.getId() == null ? null : domain.getId().value())
                .formation(domain.getFormation())
                .validFrom(domain.getValidFrom())
                .validTo(domain.getValidTo())
                .maxDiscountPercent(domain.getMaxDiscountPercent())
                .maxDiscountBehavior(domain.getMaxDiscountBehavior())
                .entries(toEntryEmbeddables(domain.getEntries()))
                .build();
    }

    private List<PriceTableEntry> toEntries(final List<PriceTableEntryEmbeddable> embeddables) {
        if (embeddables == null) {
            return List.of();
        }
        return embeddables.stream()
                .map(e -> new PriceTableEntry(new ProductOrClassRef(e.getRefType(), e.getReferenceId()), e.getValue()))
                .toList();
    }

    private List<PriceTableEntryEmbeddable> toEntryEmbeddables(final List<PriceTableEntry> entries) {
        // Hibernate merges a detached entity's collections in place (clear + addAll), so
        // this must stay mutable rather than an immutable Stream.toList().
        return entries.stream()
                .map(entry -> PriceTableEntryEmbeddable.builder()
                        .refType(entry.ref().type())
                        .referenceId(entry.ref().referenceId())
                        .value(entry.value())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
