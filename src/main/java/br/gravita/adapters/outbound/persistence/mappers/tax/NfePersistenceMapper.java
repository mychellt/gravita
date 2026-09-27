package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfeItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.NfeItemTaxLineEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.NfeJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.NfeTransportInfo;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface NfePersistenceMapper {

	default NfeDocument toDomain(final NfeJpaEntity entity) {
		List<NfeItem> items = toItems(entity.getItems(), entity.getTaxLines());
		TaxCalculationTotals totals = TaxCalculationTotals.from(items.stream().map(NfeItem::taxBreakdown).toList());
		return NfeDocument.of(
				NfeDocumentId.of(entity.getId()),
				CompanyId.of(entity.getIssuerCompanyId()),
				entity.getOriginSalesOrderId(),
				NaturezaOperacao.valueOf(entity.getNaturezaOperacao()),
				new Cfop(entity.getCfop()),
				toRecipient(entity),
				items,
				entity.getFreight(),
				entity.getInsurance(),
				entity.getOtherExpenses(),
				toTransport(entity),
				entity.getReferencedAccessKey(),
				entity.getAdditionalInfo(),
				totals,
				entity.getStatus(),
				entity.getDocumentCreatedAt(),
				entity.getDocumentSeries(),
				entity.getDocumentNumber(),
				entity.getAccessKey(),
				entity.getSefazProtocol());
	}

	default NfeJpaEntity toEntity(final NfeDocument domain) {
		NfeRecipient recipient = domain.getRecipient();
		NfeTransportInfo transport = domain.getTransport();
		return NfeJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.issuerCompanyId(domain.getIssuerCompanyId().value())
				.originSalesOrderId(domain.getOriginSalesOrderId())
				.naturezaOperacao(domain.getNaturezaOperacao().name())
				.cfop(domain.getCfop().code())
				.recipientPersonId(recipient.personRef() == null ? null : recipient.personRef().id())
				.recipientDocument(recipient.document().number())
				.recipientPersonType(recipient.document().personType())
				.recipientName(recipient.name())
				.recipientStateRegistration(recipient.stateRegistration())
				.recipientState(recipient.state())
				.freight(domain.getFreight())
				.insurance(domain.getInsurance())
				.otherExpenses(domain.getOtherExpenses())
				.transportModality(transport == null ? null : transport.modality())
				.transportCarrier(transport == null ? null : transport.carrier())
				.transportVolume(transport == null ? null : transport.volume())
				.transportGrossWeight(transport == null ? null : transport.grossWeight())
				.transportNetWeight(transport == null ? null : transport.netWeight())
				.transportRntrc(transport == null ? null : transport.rntrc())
				.referencedAccessKey(domain.getReferencedAccessKey())
				.additionalInfo(domain.getAdditionalInfo())
				.status(domain.getStatus())
				.documentCreatedAt(domain.getCreatedAt())
				.documentSeries(domain.getDocumentSeries())
				.documentNumber(domain.getDocumentNumber())
				.accessKey(domain.getAccessKey())
				.sefazProtocol(domain.getSefazProtocol())
				.items(toItemEmbeddables(domain.getItems()))
				.taxLines(toTaxLineEmbeddables(domain.getItems()))
				.build();
	}

	private NfeRecipient toRecipient(NfeJpaEntity entity) {
		PersonRef personRef = entity.getRecipientPersonId() == null ? null : new PersonRef(entity.getRecipientPersonId());
		Document document = new Document(entity.getRecipientDocument(), entity.getRecipientPersonType());
		return new NfeRecipient(personRef, document, entity.getRecipientName(), entity.getRecipientStateRegistration(),
				entity.getRecipientState());
	}

	// Absence of transport data is represented as every transport_* column
	// being null, rather than a separate "has transport" flag.
	private NfeTransportInfo toTransport(NfeJpaEntity entity) {
		if (entity.getTransportModality() == null && entity.getTransportCarrier() == null
				&& entity.getTransportVolume() == null && entity.getTransportGrossWeight() == null
				&& entity.getTransportNetWeight() == null && entity.getTransportRntrc() == null) {
			return null;
		}
		return new NfeTransportInfo(entity.getTransportModality(), entity.getTransportCarrier(),
				entity.getTransportVolume(), entity.getTransportGrossWeight(), entity.getTransportNetWeight(),
				entity.getTransportRntrc());
	}

	// TaxCalculationTotals is not persisted separately - it's rebuilt from the
	// item tax lines below via TaxCalculationTotals.from(...).
	private List<NfeItem> toItems(List<NfeItemEmbeddable> items, List<NfeItemTaxLineEmbeddable> taxLines) {
		if (items == null) {
			return List.of();
		}
		return items.stream()
				.sorted(Comparator.comparing(NfeItemEmbeddable::getItemIndex))
				.map(item -> {
					List<TaxLineBreakdown> lines = taxLines == null ? List.of()
							: taxLines.stream().filter(line -> line.getItemIndex().equals(item.getItemIndex()))
									.map(line -> new TaxLineBreakdown(line.getTaxType(), line.getBase(),
											line.getRatePercentage(), line.getComputedAmount(), line.getFinalAmount(),
											line.isOverridden(), line.getOverrideJustification()))
									.toList();
					ItemTaxBreakdown breakdown = new ItemTaxBreakdown(item.getItemIndex(),
							item.getProductId().toString(), lines);
					return new NfeItem(item.getProductId(), item.getDescription(), item.getQuantity(),
							item.getUnitPrice(), item.getDiscount(), breakdown);
				})
				.toList();
	}

	// Hibernate merges a detached entity's collections in place (clear + addAll), so
	// these must stay mutable rather than an immutable Stream.toList().
	private List<NfeItemEmbeddable> toItemEmbeddables(List<NfeItem> items) {
		List<NfeItemEmbeddable> result = new ArrayList<>();
		for (int index = 0; index < items.size(); index++) {
			NfeItem item = items.get(index);
			result.add(NfeItemEmbeddable.builder()
					.itemIndex(index)
					.productId(item.productId())
					.description(item.description())
					.quantity(item.quantity())
					.unitPrice(item.unitPrice())
					.discount(item.discount())
					.build());
		}
		return result;
	}

	private List<NfeItemTaxLineEmbeddable> toTaxLineEmbeddables(List<NfeItem> items) {
		List<NfeItemTaxLineEmbeddable> result = new ArrayList<>();
		for (int index = 0; index < items.size(); index++) {
			for (TaxLineBreakdown line : items.get(index).taxBreakdown().taxLines()) {
				result.add(NfeItemTaxLineEmbeddable.builder()
						.itemIndex(index)
						.taxType(line.taxType())
						.base(line.base())
						.ratePercentage(line.ratePercentage())
						.computedAmount(line.computedAmount())
						.finalAmount(line.finalAmount())
						.overridden(line.overridden())
						.overrideJustification(line.overrideJustification())
						.build());
			}
		}
		return result;
	}
}
