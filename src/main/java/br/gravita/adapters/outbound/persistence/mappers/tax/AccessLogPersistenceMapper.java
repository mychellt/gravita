package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.AccessLogJpaEntity;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.UserId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.UUID;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface AccessLogPersistenceMapper {

	AccessLog map(final AccessLogJpaEntity entity);

	@Mapping(target = "userId", source = "userId.value")
	AccessLogJpaEntity map(final AccessLog domain);

	@Mapping(target = "value", source = "id")
	UserId mapUserId(final UUID id);
}
