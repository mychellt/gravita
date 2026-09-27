package br.gravita.core.ports.outbound.sales;

import br.gravita.core.domain.sales.FollowUpTask;

public interface SendFollowUpAlertPort {
	void send(FollowUpTask task);
}
