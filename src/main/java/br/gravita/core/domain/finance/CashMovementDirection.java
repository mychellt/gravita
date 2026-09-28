package br.gravita.core.domain.finance;

/** Which way cash moves relative to the internal cash box. */
public enum CashMovementDirection {
	/** Cash leaves the box and is deposited in the bank. */
	TO_BANK,
	/** Cash is withdrawn from the bank and enters the box. */
	FROM_BANK
}
