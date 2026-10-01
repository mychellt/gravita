package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.QuoteItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.sales.QuoteJpaEntity;
import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.QuoteItem;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface QuotePersistenceMapper {

	Quote map(final QuoteJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	QuoteJpaEntity map(final Quote domain);

	QuoteItem map(final QuoteItemEmbeddable embeddable);

	QuoteItemEmbeddable map(final QuoteItem item);

	@Mapping(target = "value", source = "id")
	QuoteId map(final UUID id);
}
