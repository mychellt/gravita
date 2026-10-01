package br.gravita.core.domain.tax;

/** When a tax rule is withheld at source by the tomador instead of being collected by the provider. */
public enum WithholdingMode {
	NEVER,
	/** Withheld only when the tomador is a company (PJ); a PF tomador is not a withholding agent. */
	TOMADOR_COMPANY,
	ALWAYS
}
