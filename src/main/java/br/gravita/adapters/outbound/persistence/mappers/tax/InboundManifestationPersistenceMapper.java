package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundManifestationJpaEntity;
import br.gravita.core.domain.tax.InboundManifestation;
import br.gravita.core.domain.tax.InboundManifestationId;
import br.gravita.core.domain.tax.InboundNfeId;
import java.util.Optional;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface InboundManifestationPersistenceMapper {

	InboundManifestation map(final InboundManifestationJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "inboundNfeId", source = "inboundNfeId", qualifiedByName = "unwrapInboundNfeId")
	InboundManifestationJpaEntity map(final InboundManifestation domain);

	@Mapping(target = "value", source = "id")
	InboundManifestationId mapInboundManifestationId(final UUID id);

	@Mapping(target = "value", source = "id")
	InboundNfeId mapInboundNfeId(final UUID id);

	// InboundManifestation exposes its optional InboundNfeId as an Optional, which MapStruct cannot unwrap itself.
	@Named("unwrapInboundNfeId")
	static UUID unwrapInboundNfeId(final Optional<InboundNfeId> inboundNfeId) {
		return inboundNfeId.map(InboundNfeId::value).orElse(null);
	}
}
