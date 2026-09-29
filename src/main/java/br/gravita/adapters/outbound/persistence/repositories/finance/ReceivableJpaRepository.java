package br.gravita.adapters.outbound.persistence.repositories.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.ReceivableJpaEntity;
import br.gravita.core.domain.finance.ReceivableStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableJpaRepository extends JpaRepository<ReceivableJpaEntity, UUID> {

	List<ReceivableJpaEntity> findByOriginDocumentRefOrderByInstallmentNumber(UUID originDocumentRef);

	List<ReceivableJpaEntity> findByCustomerId(UUID customerId);

	List<ReceivableJpaEntity> findByCustomerIdAndStatusIn(UUID customerId, Collection<ReceivableStatus> statuses);

	List<ReceivableJpaEntity> findByStatusInAndDueDateLessThanEqualOrderByDueDateAscIdAsc(
			Collection<ReceivableStatus> statuses, LocalDate until);

	List<ReceivableJpaEntity> findByCustomerIdAndStatusInAndDueDateLessThanEqualOrderByDueDateAscIdAsc(
			UUID customerId, Collection<ReceivableStatus> statuses, LocalDate until);
}
