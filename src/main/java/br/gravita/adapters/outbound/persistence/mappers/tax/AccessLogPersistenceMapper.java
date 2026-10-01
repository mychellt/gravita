package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.AccessLogJpaEntity;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.UserId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

import java.util.UUID;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface AccessLogPersistenceMapper {

	AccessLog map(final AccessLogJpaEntity entity);

	AccessLogJpaEntity map(final AccessLog domain);

	default UUID map(final UserId userId) {
		return userId == null ? null : userId.value();
	}

	default UserId map(final UUID id) {
		return id == null ? null : UserId.of(id);
	}
}
