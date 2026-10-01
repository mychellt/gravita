package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.PosSessionJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PosSessionPersistenceMapper {

	PosSession map(final PosSessionJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "companyId", source = "companyId.value")
	PosSessionJpaEntity map(final PosSession domain);

	@Mapping(target = "value", source = "id")
	PosSessionId mapPosSessionId(final UUID id);

	@Mapping(target = "value", source = "id")
	CompanyId mapCompanyId(final UUID id);
}
