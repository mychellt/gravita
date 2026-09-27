package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeConferenceItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeConferenceItem;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface InboundNfePersistenceMapper {

	default InboundNfe toDomain(final InboundNfeJpaEntity entity) {
		return InboundNfe.of(
				InboundNfeId.of(entity.getId()),
				CompanyId.of(entity.getCompanyId()),
				entity.getAccessKey(),
				entity.getSeries(),
				entity.getNumber(),
				Document.cnpj(entity.getSupplierDocument()),
				entity.getSupplierName(),
				entity.getIssuedAt(),
				toItems(entity.getItems()),
				new InboundNfeTotals(
						entity.getProductsValue(),
						entity.getFreightValue(),
						entity.getInsuranceValue(),
						entity.getDiscountValue(),
						entity.getOtherExpensesValue(),
						entity.getIcmsValue(),
						entity.getIpiValue(),
						entity.getPisValue(),
						entity.getCofinsValue(),
						entity.getTotalValue()),
				entity.getXmlStorageRef(),
				entity.getStatus(),
				entity.getImportedAt(),
				toConferenceResult(entity.getConferenceResult()));
	}

	default InboundNfeJpaEntity toEntity(final InboundNfe domain) {
		InboundNfeTotals totals = domain.getTotals();
		return InboundNfeJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.companyId(domain.getCompanyId().value())
				.accessKey(domain.getAccessKey())
				.series(domain.getSeries())
				.number(domain.getNumber())
				.supplierDocument(domain.getSupplierDocument().number())
				.supplierName(domain.getSupplierName())
				.issuedAt(domain.getIssuedAt())
				.productsValue(totals.productsValue())
				.freightValue(totals.freightValue())
				.insuranceValue(totals.insuranceValue())
				.discountValue(totals.discountValue())
				.otherExpensesValue(totals.otherExpensesValue())
				.icmsValue(totals.icmsValue())
				.ipiValue(totals.ipiValue())
				.pisValue(totals.pisValue())
				.cofinsValue(totals.cofinsValue())
				.totalValue(totals.totalValue())
				.xmlStorageRef(domain.getXmlStorageRef())
				.status(domain.getStatus())
				.importedAt(domain.getImportedAt())
				.items(toItemEmbeddables(domain.getItems()))
				.conferenceResult(toConferenceEmbeddables(domain.getConferenceResult()))
				.build();
	}

	private List<InboundNfeItem> toItems(final List<InboundNfeItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new InboundNfeItem(e.getSupplierProductCode(), e.getDescription(), e.getNcm(), e.getCfop(),
						e.getUnit(), e.getQuantity(), e.getUnitValue(), e.getTotalValue(), e.getIcmsValue(),
						e.getIpiValue(), e.getPisValue(), e.getCofinsValue()))
				.toList();
	}

	private List<InboundNfeItemEmbeddable> toItemEmbeddables(final List<InboundNfeItem> items) {
		return items.stream()
				.map(item -> InboundNfeItemEmbeddable.builder()
						.supplierProductCode(item.supplierProductCode())
						.description(item.description())
						.ncm(item.ncm())
						.cfop(item.cfop())
						.unit(item.unit())
						.quantity(item.quantity())
						.unitValue(item.unitValue())
						.totalValue(item.totalValue())
						.icmsValue(item.icmsValue())
						.ipiValue(item.ipiValue())
						.pisValue(item.pisValue())
						.cofinsValue(item.cofinsValue())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private List<InboundNfeConferenceItem> toConferenceResult(final List<InboundNfeConferenceItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new InboundNfeConferenceItem(e.getItemRef(), e.getOrderedQty(), e.getReceivedQty()))
				.toList();
	}

	private List<InboundNfeConferenceItemEmbeddable> toConferenceEmbeddables(final List<InboundNfeConferenceItem> conferenceResult) {
		return conferenceResult.stream()
				.map(item -> InboundNfeConferenceItemEmbeddable.builder()
						.itemRef(item.itemRef())
						.orderedQty(item.orderedQty())
						.receivedQty(item.receivedQty())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
