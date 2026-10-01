package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.AccessLogJpaEntity;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.UserId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

import java.util.UUID;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface AccessLogPersistenceMapper {

	@Mapping(target = "userId", source = "userId", qualifiedByName = "toUuid")
	AccessLogJpaEntity toEntity(final AccessLog domain);

	default AccessLog toDomain(final AccessLogJpaEntity entity) {
		if (entity == null) {
			return null;
		}
		return AccessLog.builder()
				.id(entity.getId())
				.userId(toUserId(entity.getUserId()))
				.email(entity.getEmail())
				.event(entity.getEvent())
				.successful(entity.isSuccessful())
				.ip(entity.getIp())
				.device(entity.getDevice())
				.timestamp(entity.getTimestamp())
				.build();
	}

	@Named("toUuid")
	default UUID toUuid(UserId userId) {
		return userId == null ? null : userId.value();
	}

	default UserId toUserId(UUID id) {
		return id == null ? null : UserId.of(id);
	}
}
