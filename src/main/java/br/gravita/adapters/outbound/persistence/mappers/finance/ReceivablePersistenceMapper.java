package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.ReceivableJpaEntity;
import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ReceivablePersistenceMapper {

	@Mapping(target = "scope", source = "entity")
	Receivable map(final ReceivableJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "companyId", source = "scope.companyId")
	@Mapping(target = "branchId", source = "scope.branchId")
	@Mapping(target = "bankAccountId", source = "scope.bankAccountId")
	ReceivableJpaEntity map(final Receivable domain);

	LedgerScope mapScope(final ReceivableJpaEntity entity);

	@Mapping(target = "value", source = "id")
	ReceivableId mapReceivableId(final UUID id);
}
