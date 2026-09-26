package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfceSaleJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.PaymentEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.SaleItemEmbeddable;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.Payment;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.SaleItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface NfceSalePersistenceMapper {

	default NfceSale toDomain(final NfceSaleJpaEntity entity) {
		return NfceSale.of(
				NfceSaleId.of(entity.getId()),
				PosSessionId.of(entity.getSessionId()),
				toItems(entity.getItems()),
				entity.getTotalDiscount(),
				toPayments(entity.getPayments()),
				entity.getChangeGiven(),
				entity.getCustomerCpf(),
				entity.getStatus(),
				entity.getRegisteredAt(),
				entity.getDocumentSeries(),
				entity.getDocumentNumber(),
				entity.getAccessKey(),
				entity.getSefazProtocol(),
				entity.isContingencyMode());
	}

	default NfceSaleJpaEntity toEntity(final NfceSale domain) {
		return NfceSaleJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.sessionId(domain.getSessionId().value())
				.totalDiscount(domain.getTotalDiscount())
				.changeGiven(domain.getChangeGiven())
				.customerCpf(domain.getCustomerCpf())
				.status(domain.getStatus())
				.registeredAt(domain.getCreatedAt())
				.documentSeries(domain.getDocumentSeries())
				.documentNumber(domain.getDocumentNumber())
				.accessKey(domain.getAccessKey())
				.sefazProtocol(domain.getSefazProtocol())
				.contingencyMode(domain.isContingencyMode())
				.items(toItemEmbeddables(domain.getItems()))
				.payments(toPaymentEmbeddables(domain.getPayments()))
				.build();
	}

	private List<SaleItem> toItems(final List<SaleItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new SaleItem(e.getProductId(), e.getQuantity(), e.getUnitPrice(), e.getItemDiscount()))
				.toList();
	}

	private List<Payment> toPayments(final List<PaymentEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream().map(e -> new Payment(e.getMethod(), e.getAmount())).toList();
	}

	// Hibernate merges a detached entity's collections in place (clear + addAll), so
	// these must stay mutable rather than an immutable Stream.toList().
	private List<SaleItemEmbeddable> toItemEmbeddables(final List<SaleItem> items) {
		return items.stream()
				.map(item -> SaleItemEmbeddable.builder()
						.productId(item.productId())
						.quantity(item.quantity())
						.unitPrice(item.unitPrice())
						.itemDiscount(item.itemDiscount())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private List<PaymentEmbeddable> toPaymentEmbeddables(final List<Payment> payments) {
		return payments.stream()
				.map(payment -> PaymentEmbeddable.builder()
						.method(payment.method())
						.amount(payment.amount())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
