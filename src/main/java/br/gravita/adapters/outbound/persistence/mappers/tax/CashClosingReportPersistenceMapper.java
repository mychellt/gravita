package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.CashClosingReportJpaEntity;
import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.tax.CashClosingReport;
import br.gravita.core.domain.tax.CashClosingReportId;
import br.gravita.core.domain.tax.PosSessionId;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CashClosingReportPersistenceMapper {

	default CashClosingReport toDomain(final CashClosingReportJpaEntity entity) {
		return CashClosingReport.of(
				CashClosingReportId.of(entity.getId()),
				PosSessionId.of(entity.getSessionId()),
				entity.getRegisterId(),
				entity.getOperatorId(),
				entity.getOpeningAmount(),
				entity.getExpectedAmountsByPaymentMethod(),
				entity.getCountedAmountsByPaymentMethod(),
				entity.getTotalSangriaAmount(),
				entity.getTotalSuprimentoAmount(),
				entity.getSaleCount(),
				entity.getOpenedAt(),
				entity.getClosedAt());
	}

	default CashClosingReportJpaEntity toEntity(final CashClosingReport domain) {
		return CashClosingReportJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.sessionId(domain.getSessionId().value())
				.registerId(domain.getRegisterId())
				.operatorId(domain.getOperatorId())
				.openingAmount(domain.getOpeningAmount())
				.expectedAmountsByPaymentMethod(toMutableMap(domain.getExpectedAmountsByPaymentMethod()))
				.countedAmountsByPaymentMethod(toMutableMap(domain.getCountedAmountsByPaymentMethod()))
				.totalSangriaAmount(domain.getTotalSangriaAmount())
				.totalSuprimentoAmount(domain.getTotalSuprimentoAmount())
				.saleCount(domain.getSaleCount())
				.openedAt(domain.getOpenedAt())
				.closedAt(domain.getClosedAt())
				.build();
	}

	private Map<PaymentMethodType, BigDecimal> toMutableMap(final Map<PaymentMethodType, BigDecimal> source) {
		Map<PaymentMethodType, BigDecimal> map = new EnumMap<>(PaymentMethodType.class);
		if (source != null) {
			map.putAll(source);
		}
		return map;
	}
}
