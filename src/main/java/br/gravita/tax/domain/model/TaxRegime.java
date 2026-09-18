package br.gravita.tax.domain.model;

/**
 * The three Brazilian corporate tax regimes. The engine never branches on
 * this value directly — it only flows through to the rate-table lookup so
 * the parameterized data selects which rows apply (doc §13).
 */
public enum TaxRegime {
	SIMPLES_NACIONAL,
	LUCRO_PRESUMIDO,
	LUCRO_REAL
}
