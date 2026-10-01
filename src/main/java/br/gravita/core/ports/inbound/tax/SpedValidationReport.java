package br.gravita.core.ports.inbound.tax;

import java.util.List;
import java.util.Objects;

/**
 * What the validation of the mandatory EFD records found. An {@link Severity#ERROR} means the file cannot be
 * produced; a {@link Severity#WARNING} is a gap the file goes out with, because {@code tax} has no source for the
 * data.
 */
public record SpedValidationReport(List<Issue> issues) {

	public SpedValidationReport {
		issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
	}

	public List<Issue> errors() {
		return issues.stream().filter(issue -> issue.severity() == Severity.ERROR).toList();
	}

	public List<Issue> warnings() {
		return issues.stream().filter(issue -> issue.severity() == Severity.WARNING).toList();
	}

	public boolean hasErrors() {
		return issues.stream().anyMatch(issue -> issue.severity() == Severity.ERROR);
	}

	public enum Severity {
		ERROR, WARNING
	}

	/** {@code record} is the EFD register (e.g. {@code C100}), {@code reference} the document or party it came from. */
	public record Issue(Severity severity, String record, String reference, String message) {
	}
}
