package br.gravita.core.ports.inbound.reporting;

/** Which way a booked document moved goods: {@code ENTRY} is a purchase received, {@code EXIT} a sale issued. */
public enum FiscalBookFlow {
	ENTRY,
	EXIT
}
