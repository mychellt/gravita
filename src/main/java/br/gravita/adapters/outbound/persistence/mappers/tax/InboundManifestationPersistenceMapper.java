package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundManifestationJpaEntity;
import br.gravita.core.domain.tax.InboundManifestation;
import br.gravita.core.domain.tax.InboundManifestationId;
import br.gravita.core.domain.tax.InboundNfeId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface InboundManifestationPersistenceMapper {

	default InboundManifestation toDomain(final InboundManifestationJpaEntity entity) {
		return InboundManifestation.of(
				InboundManifestationId.of(entity.getId()),
				entity.getAccessKey(),
				entity.getType(),
				entity.getInboundNfeId() == null ? null : InboundNfeId.of(entity.getInboundNfeId()),
				entity.getSefazProtocol(),
				entity.getManifestedAt());
	}

	default InboundManifestationJpaEntity toEntity(final InboundManifestation domain) {
		return InboundManifestationJpaEntity.builder()
				.id(domain.getId().value())
				.accessKey(domain.getAccessKey())
				.type(domain.getType())
				.inboundNfeId(domain.getInboundNfeId().map(InboundNfeId::value).orElse(null))
				.sefazProtocol(domain.getSefazProtocol())
				.manifestedAt(domain.getManifestedAt())
				.build();
	}
}
