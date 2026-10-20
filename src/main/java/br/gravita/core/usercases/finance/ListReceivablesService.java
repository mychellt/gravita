package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.ports.inbound.finance.ListReceivablesUseCase;
import br.gravita.core.ports.inbound.finance.ReceivableListItem;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@UseCase
public class ListReceivablesService implements ListReceivablesUseCase {

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final CallerCompanyResolver callerCompanyResolver;

	public ListReceivablesService(final ReceivableRepositoryPort receivableRepositoryPort,
			final SettlementRepositoryPort settlementRepositoryPort, final CustomerRepositoryPort customerRepositoryPort,
			final CallerCompanyResolver callerCompanyResolver) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
		this.customerRepositoryPort = customerRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public List<ReceivableListItem> execute(final Context context) {
		final Map<UUID, String> customerNames = callerCompanyResolver.resolve(context)
				.map(customerRepositoryPort::findAllByCompanyId).orElse(List.of()).stream()
				.collect(Collectors.toMap(CustomerDomain::getId, CustomerDomain::getName, (first, second) -> first));
		if (customerNames.isEmpty()) {
			return List.of();
		}
		final List<Receivable> receivables = receivableRepositoryPort.findByCustomerIds(customerNames.keySet());
		final Map<ReceivableId, List<Settlement>> settlements = new HashMap<>();
		if (!receivables.isEmpty()) {
			settlementRepositoryPort.findByReceivableIds(receivables.stream().map(Receivable::getId).toList())
					.forEach(settlement -> settlements.computeIfAbsent(settlement.getReceivableId(), id -> new java.util.ArrayList<>())
							.add(settlement));
		}
		return receivables.stream()
				.map(receivable -> new ReceivableListItem(receivable, customerNames.get(receivable.getCustomerId()),
						receivable.remainingBalance(settlements.getOrDefault(receivable.getId(), List.of()))))
				.toList();
	}
}
