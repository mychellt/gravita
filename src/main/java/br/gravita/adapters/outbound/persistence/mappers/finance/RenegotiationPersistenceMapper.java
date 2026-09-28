package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.RenegotiationJpaEntity;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.domain.finance.RenegotiationId;
import java.util.ArrayList;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface RenegotiationPersistenceMapper {

	default Renegotiation toDomain(final RenegotiationJpaEntity entity) {
		return Renegotiation.of(RenegotiationId.of(entity.getId()), entity.getCustomerId(),
				entity.getOriginalReceivableIds().stream().map(ReceivableId::of).toList(),
				entity.getNewReceivableIds().stream().map(ReceivableId::of).toList(), entity.getRenegotiatedAt());
	}

	default RenegotiationJpaEntity toEntity(final Renegotiation domain) {
		return RenegotiationJpaEntity.builder()
				.id(domain.getId().value())
				.customerId(domain.getCustomerId())
				.renegotiatedAt(domain.getCreatedAt())
				.originalReceivableIds(new ArrayList<>(domain.getOriginalReceivableIds().stream()
						.map(ReceivableId::value).toList()))
				.newReceivableIds(new ArrayList<>(domain.getNewReceivableIds().stream()
						.map(ReceivableId::value).toList()))
				.build();
	}
}
