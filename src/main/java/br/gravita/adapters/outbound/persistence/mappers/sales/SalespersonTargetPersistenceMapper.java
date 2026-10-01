package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalespersonTargetJpaEntity;
import br.gravita.core.domain.sales.SalespersonTarget;
import java.time.YearMonth;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SalespersonTargetPersistenceMapper {

	@Mapping(target = "month", source = "month", qualifiedByName = "toYearMonth")
	SalespersonTarget map(final SalespersonTargetJpaEntity entity);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "month", source = "domain.month", qualifiedByName = "fromYearMonth")
	SalespersonTargetJpaEntity map(final SalespersonTarget domain, final UUID id);

	@Named("toYearMonth")
	static YearMonth toYearMonth(final String month) {
		return YearMonth.parse(month);
	}

	@Named("fromYearMonth")
	static String fromYearMonth(final YearMonth month) {
		return month.toString();
	}
}
