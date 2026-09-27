package br.gravita.core.domain.tax;

/**
 * NFe operation-nature categories (doc §3.1: "free CFOP registration with
 * automatic mapping by type"). This is the type the CFOP registry keys on
 * (UC-M2-01, AC2) and the flag that decides whether a
 * {@code referencedAccessKey} is mandatory (AC6).
 */
public enum NaturezaOperacao {
	VENDA,
	DEVOLUCAO,
	NOTA_COMPLEMENTAR,
	TRANSFERENCIA,
	REMESSA,
	OUTRA;

	/**
	 * AC6: returns and complementary notes must reference the original NF's
	 * access key.
	 */
	public boolean requiresReferencedAccessKey() {
		return this == DEVOLUCAO || this == NOTA_COMPLEMENTAR;
	}
}
