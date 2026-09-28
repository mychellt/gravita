package br.gravita.adapters.outbound.persistence.repositories.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.PayableJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.finance.ReceivableJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.finance.SettlementJpaEntity;
import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.finance.ReceivableStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * The cash-flow queries, whose restrictions depend on which filters are given.
 * The JPQL is assembled from the filters that are set, so an unset filter adds
 * neither a clause nor a (null-typed) parameter.
 */
@Repository
public class CashFlowQueryRepository {

	@PersistenceContext
	private EntityManager entityManager;

	public List<ReceivableJpaEntity> findOutstandingReceivables(LocalDate until, CashFlowFilter filter) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("statuses", List.of(ReceivableStatus.OPEN, ReceivableStatus.PARTIALLY_SETTLED));
		parameters.put("until", until);
		String jpql = "select r from ReceivableJpaEntity r where r.status in :statuses and r.dueDate <= :until"
				+ scopeClauses("r", filter, parameters) + " order by r.dueDate, r.id";
		return typed(jpql, ReceivableJpaEntity.class, parameters).getResultList();
	}

	public List<PayableJpaEntity> findOutstandingPayables(LocalDate until, CashFlowFilter filter) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("statuses", List.of(PayableStatus.OPEN, PayableStatus.APPROVED));
		parameters.put("until", until);
		String jpql = "select p from PayableJpaEntity p where p.status in :statuses and p.dueDate <= :until"
				+ scopeClauses("p", filter, parameters) + costCenterClause("p", filter, parameters)
				+ " order by p.dueDate, p.id";
		return typed(jpql, PayableJpaEntity.class, parameters).getResultList();
	}

	public List<SettlementJpaEntity> findRealizedSettlements(Instant from, Instant until, CashFlowFilter filter) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("from", from);
		parameters.put("until", until);
		StringBuilder jpql = new StringBuilder(
				"select s from SettlementJpaEntity s where s.timestamp >= :from and s.timestamp < :until");

		String receivableScope = scopeClauses("r", filter, parameters);
		String payableScope = scopeClauses("p", filter, parameters) + costCenterClause("p", filter, parameters);
		boolean narrowed = !receivableScope.isEmpty() || !payableScope.isEmpty();
		if (narrowed) {
			// Receivables are never charged to a cost center, so that filter leaves only the payable side.
			String payableSide = "exists (select p.id from PayableJpaEntity p where p.id = s.payableId" + payableScope
					+ ")";
			if (filter.costCenterId() != null) {
				jpql.append(" and ").append(payableSide);
			}
			else {
				jpql.append(" and (exists (select r.id from ReceivableJpaEntity r where r.id = s.receivableId")
						.append(receivableScope).append(") or ").append(payableSide).append(")");
			}
		}
		jpql.append(" order by s.timestamp, s.id");
		return typed(jpql.toString(), SettlementJpaEntity.class, parameters).getResultList();
	}

	private static String scopeClauses(String alias, CashFlowFilter filter, Map<String, Object> parameters) {
		StringBuilder clauses = new StringBuilder();
		if (filter.companyId() != null) {
			clauses.append(" and ").append(alias).append(".companyId = :companyId");
			parameters.put("companyId", filter.companyId());
		}
		if (filter.branchId() != null) {
			clauses.append(" and ").append(alias).append(".branchId = :branchId");
			parameters.put("branchId", filter.branchId());
		}
		if (filter.bankAccountId() != null) {
			clauses.append(" and ").append(alias).append(".bankAccountId = :bankAccountId");
			parameters.put("bankAccountId", filter.bankAccountId());
		}
		return clauses.toString();
	}

	private static String costCenterClause(String alias, CashFlowFilter filter, Map<String, Object> parameters) {
		if (filter.costCenterId() == null) {
			return "";
		}
		parameters.put("costCenterId", filter.costCenterId());
		return " and exists (select share from " + alias
				+ ".costCenterSplit share where share.costCenterId = :costCenterId)";
	}

	private <T> TypedQuery<T> typed(String jpql, Class<T> type, Map<String, Object> parameters) {
		TypedQuery<T> query = entityManager.createQuery(jpql, type);
		parameters.forEach(query::setParameter);
		return query;
	}
}
