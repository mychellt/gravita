package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.AccessLogJpaEntity;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.UserId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

import java.util.UUID;

@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface AccessLogPersistenceMapper {

	@Mapping(target = "userId", source = "userId", qualifiedByName = "toUuid")
	AccessLogJpaEntity map(final AccessLog domain);

	@Mapping(target = "userId", source = "userId", qualifiedByName = "toUserId")
	AccessLog map(final AccessLogJpaEntity entity);

	@Named("toUuid")
	default UUID toUuid(UserId userId) {
		return userId == null ? null : userId.value();
	}

	@Named("toUserId")
	default UserId toUserId(UUID id) {
		return id == null ? null : UserId.of(id);
	}
}
