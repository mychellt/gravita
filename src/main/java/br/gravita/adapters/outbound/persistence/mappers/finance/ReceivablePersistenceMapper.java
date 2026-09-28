package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.ReceivableJpaEntity;
import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ReceivablePersistenceMapper {

	default Receivable toDomain(final ReceivableJpaEntity entity) {
		return Receivable.of(ReceivableId.of(entity.getId()), entity.getCustomerId(), entity.getOrigin(),
				entity.getAmount(), entity.getDueDate(), entity.getInstallments(), entity.getStatus(),
				entity.getOriginDocumentRef(), entity.getInstallmentNumber(),
				new LedgerScope(entity.getCompanyId(), entity.getBranchId(), entity.getBankAccountId()));
	}

	default ReceivableJpaEntity toEntity(final Receivable domain) {
		return ReceivableJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.customerId(domain.getCustomerId())
				.origin(domain.getOrigin())
				.amount(domain.getAmount())
				.dueDate(domain.getDueDate())
				.installments(domain.getInstallments())
				.status(domain.getStatus())
				.originDocumentRef(domain.getOriginDocumentRef())
				.installmentNumber(domain.getInstallmentNumber())
				.companyId(domain.getScope().companyId())
				.branchId(domain.getScope().branchId())
				.bankAccountId(domain.getScope().bankAccountId())
				.build();
	}
}
