package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.CashClosingReportJpaEntity;
import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.tax.CashClosingReport;
import br.gravita.core.domain.tax.CashClosingReportId;
import br.gravita.core.domain.tax.PosSessionId;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CashClosingReportPersistenceMapper {

	CashClosingReport map(final CashClosingReportJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "sessionId", source = "sessionId.value")
	@Mapping(target = "expectedAmountsByPaymentMethod", qualifiedByName = "toMutableMap")
	@Mapping(target = "countedAmountsByPaymentMethod", qualifiedByName = "toMutableMap")
	CashClosingReportJpaEntity map(final CashClosingReport domain);

	@Mapping(target = "value", source = "id")
	CashClosingReportId mapCashClosingReportId(final UUID id);

	@Mapping(target = "value", source = "id")
	PosSessionId mapPosSessionId(final UUID id);

	// JPA needs a mutable map it can merge into in place; a null source yields an empty one.
	@Named("toMutableMap")
	static Map<PaymentMethodType, BigDecimal> toMutableMap(final Map<PaymentMethodType, BigDecimal> source) {
		Map<PaymentMethodType, BigDecimal> map = new EnumMap<>(PaymentMethodType.class);
		if (source != null) {
			map.putAll(source);
		}
		return map;
	}
}
