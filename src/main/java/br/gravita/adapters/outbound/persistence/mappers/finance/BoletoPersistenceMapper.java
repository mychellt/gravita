package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.BoletoJpaEntity;
import br.gravita.core.domain.finance.Boleto;
import br.gravita.core.domain.finance.BoletoId;
import br.gravita.core.domain.finance.ReceivableId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface BoletoPersistenceMapper {

	default Boleto toDomain(final BoletoJpaEntity entity) {
		return Boleto.of(BoletoId.of(entity.getId()), ReceivableId.of(entity.getReceivableId()),
				entity.getBankIntegration(), entity.getBarcodeLine(), entity.getStatus());
	}

	default BoletoJpaEntity toEntity(final Boleto domain) {
		return BoletoJpaEntity.builder()
				.id(domain.getId().value())
				.receivableId(domain.getReceivableId().value())
				.bankIntegration(domain.getBankIntegration())
				.barcodeLine(domain.getBarcodeLine())
				.status(domain.getStatus())
				.build();
	}
}
