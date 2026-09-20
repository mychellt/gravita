package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.domain.model.PriceTable;
import br.gravita.masterdata.domain.model.PriceTableEntry;
import br.gravita.masterdata.domain.model.PriceTableId;
import br.gravita.masterdata.domain.model.ProductOrClassRef;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class PriceTablePersistenceMapper {

	PriceTable toDomain(PriceTableJpaEntity entity) {
		return PriceTable.of(
				PriceTableId.of(entity.getId()),
				entity.getFormation(),
				entity.getValidFrom(),
				entity.getValidTo(),
				entity.getMaxDiscountPercent(),
				entity.getMaxDiscountBehavior(),
				toEntries(entity.getEntries()));
	}

	PriceTableJpaEntity toEntity(PriceTable domain) {
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

	private List<PriceTableEntry> toEntries(List<PriceTableEntryEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new PriceTableEntry(new ProductOrClassRef(e.getRefType(), e.getReferenceId()), e.getValue()))
				.toList();
	}

	private List<PriceTableEntryEmbeddable> toEntryEmbeddables(List<PriceTableEntry> entries) {
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
