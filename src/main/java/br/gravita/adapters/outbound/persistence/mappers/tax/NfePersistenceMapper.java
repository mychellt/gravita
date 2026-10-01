package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfeCorrectionLetterEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.NfeItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.NfeItemTaxLineEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.NfeJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.CorrectionLetter;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
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
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface NfePersistenceMapper {

	@Mapping(target = "recipient", source = "entity")
	@Mapping(target = "items", source = "entity", qualifiedByName = "toItems")
	@Mapping(target = "transport", source = "entity",
			conditionExpression = "java(entity.getTransportModality() != null || entity.getTransportCarrier() != null"
					+ " || entity.getTransportVolume() != null || entity.getTransportGrossWeight() != null"
					+ " || entity.getTransportNetWeight() != null || entity.getTransportRntrc() != null)")
	@Mapping(target = "taxTotals", source = "entity", qualifiedByName = "toTaxTotals")
	@Mapping(target = "createdAt", source = "documentCreatedAt")
	@Mapping(target = "correctionLetters", source = "correctionLetters", qualifiedByName = "toCorrectionLetters")
	NfeDocument map(final NfeJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "issuerCompanyId", source = "issuerCompanyId.value")
	@Mapping(target = "cfop", source = "cfop.code")
	@Mapping(target = "recipientPersonId", source = "recipient.personRef.id")
	@Mapping(target = "recipientDocument", source = "recipient.document.number")
	@Mapping(target = "recipientPersonType", source = "recipient.document.personType")
	@Mapping(target = "recipientName", source = "recipient.name")
	@Mapping(target = "recipientStateRegistration", source = "recipient.stateRegistration")
	@Mapping(target = "recipientState", source = "recipient.state")
	@Mapping(target = "transportModality", source = "transport.modality")
	@Mapping(target = "transportCarrier", source = "transport.carrier")
	@Mapping(target = "transportVolume", source = "transport.volume")
	@Mapping(target = "transportGrossWeight", source = "transport.grossWeight")
	@Mapping(target = "transportNetWeight", source = "transport.netWeight")
	@Mapping(target = "transportRntrc", source = "transport.rntrc")
	@Mapping(target = "documentCreatedAt", source = "createdAt")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "items", source = "items", qualifiedByName = "toItemEmbeddables")
	@Mapping(target = "taxLines", source = "items", qualifiedByName = "toTaxLineEmbeddables")
	NfeJpaEntity map(final NfeDocument domain);

	@Mapping(target = "personRef", source = "recipientPersonId")
	@Mapping(target = "document", source = "entity")
	@Mapping(target = "name", source = "recipientName")
	@Mapping(target = "stateRegistration", source = "recipientStateRegistration")
	@Mapping(target = "state", source = "recipientState")
	NfeRecipient mapRecipient(final NfeJpaEntity entity);

	@Mapping(target = "number", source = "recipientDocument")
	@Mapping(target = "personType", source = "recipientPersonType")
	Document mapRecipientDocument(final NfeJpaEntity entity);

	@Mapping(target = "modality", source = "transportModality")
	@Mapping(target = "carrier", source = "transportCarrier")
	@Mapping(target = "volume", source = "transportVolume")
	@Mapping(target = "grossWeight", source = "transportGrossWeight")
	@Mapping(target = "netWeight", source = "transportNetWeight")
	@Mapping(target = "rntrc", source = "transportRntrc")
	NfeTransportInfo mapTransport(final NfeJpaEntity entity);

	NfeCorrectionLetterEmbeddable map(final CorrectionLetter letter);

	@Mapping(target = "value", source = "id")
	NfeDocumentId mapNfeDocumentId(final UUID id);

	@Mapping(target = "value", source = "id")
	CompanyId mapCompanyId(final UUID id);

	@Mapping(target = "id", source = "id")
	PersonRef mapPersonRef(final UUID id);

	@Mapping(target = "code", source = "cfop")
	Cfop mapCfop(final String cfop);

	// The items and their tax lines are persisted as two flat collections joined by itemIndex, so they are rebuilt
	// together, in item order.
	@Named("toItems")
	static List<NfeItem> toItems(final NfeJpaEntity entity) {
		if (entity.getItems() == null) {
			return List.of();
		}
		List<NfeItemTaxLineEmbeddable> taxLines = entity.getTaxLines();
		return entity.getItems().stream()
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

	// TaxCalculationTotals is not persisted separately - it's rebuilt from the item tax lines.
	@Named("toTaxTotals")
	static TaxCalculationTotals toTaxTotals(final NfeJpaEntity entity) {
		return TaxCalculationTotals.from(toItems(entity).stream().map(NfeItem::taxBreakdown).toList());
	}

	@Named("toCorrectionLetters")
	static List<CorrectionLetter> toCorrectionLetters(final List<NfeCorrectionLetterEmbeddable> letters) {
		if (letters == null) {
			return List.of();
		}
		return letters.stream()
				.sorted(Comparator.comparing(NfeCorrectionLetterEmbeddable::getSequenceNumber))
				.map(letter -> new CorrectionLetter(letter.getSequenceNumber(), letter.getText(), letter.getProtocol(),
						letter.getIssuedAt()))
				.toList();
	}

	// Hibernate merges a detached entity's collections in place (clear + addAll), so the
	// embeddable lists below must stay mutable rather than an immutable Stream.toList().
	@Named("toItemEmbeddables")
	static List<NfeItemEmbeddable> toItemEmbeddables(final List<NfeItem> items) {
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

	@Named("toTaxLineEmbeddables")
	static List<NfeItemTaxLineEmbeddable> toTaxLineEmbeddables(final List<NfeItem> items) {
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
