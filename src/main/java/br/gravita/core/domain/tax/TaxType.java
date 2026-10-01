package br.gravita.core.domain.tax;

public enum TaxType {
	ICMS,
	ICMS_ST,
	IPI,
	PIS,
	COFINS,
	FCP,
	// Service taxes (M4 NFSe): ISS is municipal; CSLL, IRPJ and INSS only appear as withholdings at source.
	ISS,
	CSLL,
	IRPJ,
	INSS
}
