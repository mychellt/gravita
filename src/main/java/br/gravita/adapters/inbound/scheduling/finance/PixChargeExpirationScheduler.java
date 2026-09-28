package br.gravita.adapters.inbound.scheduling.finance;

import br.gravita.core.ports.inbound.finance.ExpirePixChargesUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives {@link ExpirePixChargesUseCase} (UC-M8-04) on a schedule - the use case has no REST endpoint,
 * so this poll loop is its only trigger.
 */
@Component
public class PixChargeExpirationScheduler {

	private final ExpirePixChargesUseCase expirePixChargesUseCase;

	public PixChargeExpirationScheduler(ExpirePixChargesUseCase expirePixChargesUseCase) {
		this.expirePixChargesUseCase = expirePixChargesUseCase;
	}

	@Scheduled(cron = "${gravita.finance.pix-charge.expiration-cron:0 */15 * * * *}")
	public void expire() {
		expirePixChargesUseCase.execute();
	}
}
