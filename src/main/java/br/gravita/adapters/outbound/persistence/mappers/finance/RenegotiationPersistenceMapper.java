package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.RenegotiationJpaEntity;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.domain.finance.RenegotiationId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface RenegotiationPersistenceMapper {

	@Mapping(target = "createdAt", source = "renegotiatedAt")
	Renegotiation map(final RenegotiationJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "renegotiatedAt", source = "createdAt")
	@Mapping(target = "createdAt", ignore = true)
	RenegotiationJpaEntity map(final Renegotiation domain);

	@Mapping(target = "value", source = "id")
	RenegotiationId mapRenegotiationId(final UUID id);

	@Mapping(target = "value", source = "id")
	ReceivableId mapReceivableId(final UUID id);

	static UUID unwrap(final ReceivableId id) {
		return id.value();
	}
}
