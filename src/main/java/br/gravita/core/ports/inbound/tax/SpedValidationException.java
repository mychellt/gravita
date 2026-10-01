package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;

/** The mandatory EFD records could not be populated: nothing was generated, and the report says what is missing. */
public class SpedValidationException extends BusinessRuleException {

	private final transient SpedValidationReport report;

	public SpedValidationException(SpedValidationReport report) {
		super("SPED Fiscal not generated: " + report.errors().size() + " mandatory record(s) missing or invalid");
		this.report = report;
	}

	public SpedValidationReport report() {
		return report;
	}
}
