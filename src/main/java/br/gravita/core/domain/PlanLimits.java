package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;

/** Per-plan usage ceilings. A {@code null} limit means unlimited. */
public record PlanLimits(Integer cnpjs, Integer filiais, Integer caixasPdv, Integer usuarios) {

	private static final int MINIMUM_LIMIT = 1;

	public void validate() {
		requireUnlimitedOrPositive("cnpjs", cnpjs);
		requireUnlimitedOrPositive("filiais", filiais);
		requireUnlimitedOrPositive("caixasPdv", caixasPdv);
		requireUnlimitedOrPositive("usuarios", usuarios);
	}

	private static void requireUnlimitedOrPositive(final String limit, final Integer value) {
		if (value != null && value < MINIMUM_LIMIT) {
			throw new BusinessRuleException("Plan limit '" + limit + "' must be unlimited (null) or at least " + MINIMUM_LIMIT);
		}
	}
}
