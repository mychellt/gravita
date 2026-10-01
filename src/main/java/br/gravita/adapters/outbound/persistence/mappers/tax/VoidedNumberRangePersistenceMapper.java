package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.VoidedNumberRangeJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.domain.tax.VoidedNumberRangeId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface VoidedNumberRangePersistenceMapper {

	VoidedNumberRange map(final VoidedNumberRangeJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "companyId", source = "companyId.value")
	VoidedNumberRangeJpaEntity map(final VoidedNumberRange domain);

	@Mapping(target = "value", source = "id")
	VoidedNumberRangeId mapVoidedNumberRangeId(final UUID id);

	@Mapping(target = "value", source = "id")
	CompanyId mapCompanyId(final UUID id);
}
