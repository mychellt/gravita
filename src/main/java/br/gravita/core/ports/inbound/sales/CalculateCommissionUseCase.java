package br.gravita.core.ports.inbound.sales;

import java.util.List;

public interface CalculateCommissionUseCase {
	List<CommissionView> execute(CalculateCommissionQuery query);
}
