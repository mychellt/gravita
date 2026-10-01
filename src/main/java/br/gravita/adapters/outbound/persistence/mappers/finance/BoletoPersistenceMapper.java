package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.BoletoJpaEntity;
import br.gravita.core.domain.finance.Boleto;
import br.gravita.core.domain.finance.BoletoId;
import br.gravita.core.domain.finance.ReceivableId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface BoletoPersistenceMapper {

	Boleto map(final BoletoJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "receivableId", source = "receivableId.value")
	BoletoJpaEntity map(final Boleto domain);

	@Mapping(target = "value", source = "id")
	BoletoId mapBoletoId(final UUID id);

	@Mapping(target = "value", source = "id")
	ReceivableId mapReceivableId(final UUID id);
}
