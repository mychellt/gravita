package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.DailyClosing;

/**
 * UC-M8-20: end-of-day summary of the back office's internal cash box — entries,
 * exits, opening and closing balance. Distinct from M3's PDV Z-report.
 */
public interface CloseDailyCashUseCase {

	DailyClosing execute(CloseDailyCashCommand command);
}
