package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfeDocumentItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.NfeDocumentJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.NfeTransport;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * See {@link NfeDocumentItemEmbeddable}'s javadoc: item tax lines are
 * flattened to one column per {@link TaxType} at rest, so a round trip
 * loses the per-line base/rate/override detail (kept only in-process
 * during issuance) - it never loses the final amounts.
 */
@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface NfePersistenceMapper {

	default NfeDocument toDomain(final NfeDocumentJpaEntity entity) {
		return NfeDocument.of(
				NfeDocumentId.of(entity.getId()),
				CompanyId.of(entity.getIssuerCompanyId()),
				entity.getOriginSalesOrderId(),
				entity.getNaturezaOperacao(),
				new NfeRecipient(entity.getRecipientCustomerId(), toDocument(entity), entity.getRecipientName(),
						entity.getRecipientIeIndicator(), entity.getRecipientIe(), entity.getRecipientState()),
				toItems(entity.getItems()),
				entity.getFreight(),
				entity.getInsurance(),
				entity.getOtherExpenses(),
				toTransport(entity),
				entity.getReferencedAccessKey(),
				entity.getAdditionalInfo(),
				toTaxTotals(entity),
				entity.getStatus(),
				entity.getSeries(),
				entity.getNumber(),
				entity.getAccessKey(),
				entity.getProtocol(),
				entity.getDraftedAt());
	}

	default NfeDocumentJpaEntity toEntity(final NfeDocument domain) {
		NfeRecipient recipient = domain.getRecipient();
		NfeTransport transport = domain.getTransport();
		TaxCalculationTotals totals = domain.getTaxTotals();
		Map<TaxType, BigDecimal> byType = totals.byTaxType();
		return NfeDocumentJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.issuerCompanyId(domain.getIssuerCompanyId().value())
				.originSalesOrderId(domain.getOriginSalesOrderId())
				.naturezaOperacao(domain.getNaturezaOperacao())
				.recipientCustomerId(recipient.customerId())
				.recipientDocument(recipient.document().number())
				.recipientPersonType(recipient.document().personType())
				.recipientName(recipient.name())
				.recipientIeIndicator(recipient.ieIndicator())
				.recipientIe(recipient.ie())
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
				.icmsTotal(zeroIfAbsent(byType, TaxType.ICMS))
				.icmsStTotal(zeroIfAbsent(byType, TaxType.ICMS_ST))
				.ipiTotal(zeroIfAbsent(byType, TaxType.IPI))
				.pisTotal(zeroIfAbsent(byType, TaxType.PIS))
				.cofinsTotal(zeroIfAbsent(byType, TaxType.COFINS))
				.fcpTotal(zeroIfAbsent(byType, TaxType.FCP))
				.grandTotal(totals.grandTotal())
				.status(domain.getStatus())
				.series(domain.getSeries())
				.number(domain.getNumber())
				.accessKey(domain.getAccessKey())
				.protocol(domain.getProtocol())
				.draftedAt(domain.getCreatedAt())
				.items(toItemEmbeddables(domain.getItems()))
				.build();
	}

	private Document toDocument(NfeDocumentJpaEntity entity) {
		return entity.getRecipientPersonType() == br.gravita.core.domain.shared.PersonType.COMPANY
				? Document.cnpj(entity.getRecipientDocument())
				: Document.cpf(entity.getRecipientDocument());
	}

	private NfeTransport toTransport(NfeDocumentJpaEntity entity) {
		if (entity.getTransportModality() == null) {
			return null;
		}
		return new NfeTransport(entity.getTransportModality(), entity.getTransportCarrier(),
				entity.getTransportVolume(), entity.getTransportGrossWeight(), entity.getTransportNetWeight(),
				entity.getTransportRntrc());
	}

	private TaxCalculationTotals toTaxTotals(NfeDocumentJpaEntity entity) {
		Map<TaxType, BigDecimal> byType = new EnumMap<>(TaxType.class);
		putIfPositive(byType, TaxType.ICMS, entity.getIcmsTotal());
		putIfPositive(byType, TaxType.ICMS_ST, entity.getIcmsStTotal());
		putIfPositive(byType, TaxType.IPI, entity.getIpiTotal());
		putIfPositive(byType, TaxType.PIS, entity.getPisTotal());
		putIfPositive(byType, TaxType.COFINS, entity.getCofinsTotal());
		putIfPositive(byType, TaxType.FCP, entity.getFcpTotal());
		return new TaxCalculationTotals(byType, entity.getGrandTotal());
	}

	private void putIfPositive(Map<TaxType, BigDecimal> map, TaxType type, BigDecimal value) {
		if (value != null && value.signum() != 0) {
			map.put(type, value);
		}
	}

	private BigDecimal zeroIfAbsent(Map<TaxType, BigDecimal> byType, TaxType type) {
		return byType.getOrDefault(type, BigDecimal.ZERO);
	}

	private List<NfeItem> toItems(final List<NfeDocumentItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new NfeItem(e.getProductId(), e.getQuantity(), e.getUnitPrice(), e.getDiscountPercent(),
						e.getCfop(), toBreakdown(e)))
				.toList();
	}

	private ItemTaxBreakdown toBreakdown(NfeDocumentItemEmbeddable e) {
		List<TaxLineBreakdown> lines = new ArrayList<>();
		addLineIfPositive(lines, TaxType.ICMS, e.getIcmsValue());
		addLineIfPositive(lines, TaxType.ICMS_ST, e.getIcmsStValue());
		addLineIfPositive(lines, TaxType.IPI, e.getIpiValue());
		addLineIfPositive(lines, TaxType.PIS, e.getPisValue());
		addLineIfPositive(lines, TaxType.COFINS, e.getCofinsValue());
		addLineIfPositive(lines, TaxType.FCP, e.getFcpValue());
		return new ItemTaxBreakdown(0, e.getProductId().toString(), lines);
	}

	private void addLineIfPositive(List<TaxLineBreakdown> lines, TaxType type, BigDecimal value) {
		if (value != null && value.signum() != 0) {
			lines.add(new TaxLineBreakdown(type, null, null, value, value, false, null));
		}
	}

	// Hibernate merges a detached entity's collections in place (clear + addAll), so
	// this must stay mutable rather than an immutable Stream.toList().
	private List<NfeDocumentItemEmbeddable> toItemEmbeddables(final List<NfeItem> items) {
		return items.stream()
				.map(item -> NfeDocumentItemEmbeddable.builder()
						.productId(item.productId())
						.quantity(item.quantity())
						.unitPrice(item.unitPrice())
						.discountPercent(item.discountPercent())
						.cfop(item.cfop())
						.icmsValue(amountOf(item, TaxType.ICMS))
						.icmsStValue(amountOf(item, TaxType.ICMS_ST))
						.ipiValue(amountOf(item, TaxType.IPI))
						.pisValue(amountOf(item, TaxType.PIS))
						.cofinsValue(amountOf(item, TaxType.COFINS))
						.fcpValue(amountOf(item, TaxType.FCP))
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private BigDecimal amountOf(NfeItem item, TaxType type) {
		return item.taxBreakdown().taxLines().stream()
				.filter(line -> line.taxType() == type)
				.map(TaxLineBreakdown::finalAmount)
				.findFirst()
				.orElse(BigDecimal.ZERO);
	}
}
