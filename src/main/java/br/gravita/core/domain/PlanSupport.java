package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;

public record PlanSupport(
		boolean email,
		boolean chat,
		boolean telefone,
		Integer slaHoras,
		boolean horarioComercial,
		boolean gerenteDedicado) {

	private static final int MIN_SLA_HOURS = 1;
	private static final int MAX_SLA_HOURS = 72;

	public void validate() {
		if (!email && !chat && !telefone) {
			throw new BusinessRuleException("Plan support must offer at least one channel (email, chat or telefone)");
		}
		if (slaHoras == null || slaHoras < MIN_SLA_HOURS || slaHoras > MAX_SLA_HOURS) {
			throw new BusinessRuleException(
					"Plan support slaHoras must be between " + MIN_SLA_HOURS + " and " + MAX_SLA_HOURS);
		}
	}
}
