package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.ports.inbound.finance.ExpirePixChargesUseCase;
import br.gravita.core.ports.outbound.persistence.finance.PixChargeRepositoryPort;
import java.time.Instant;
import java.util.List;

@UseCase
public class ExpirePixChargesService implements ExpirePixChargesUseCase {

	private final PixChargeRepositoryPort pixChargeRepositoryPort;

	public ExpirePixChargesService(final PixChargeRepositoryPort pixChargeRepositoryPort) {
		this.pixChargeRepositoryPort = pixChargeRepositoryPort;
	}

	@Override
	public int execute() {
		final Instant now = Instant.now();
		final List<PixCharge> due = pixChargeRepositoryPort.findPendingExpiredBefore(now);
		due.forEach(charge -> pixChargeRepositoryPort.save(charge.expireIfDue(now)));
		return due.size();
	}
}
