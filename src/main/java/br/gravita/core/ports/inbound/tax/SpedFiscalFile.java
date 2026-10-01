package br.gravita.core.ports.inbound.tax;

/** The EFD ICMS/IPI file of a period, as bytes ready to be saved (ISO-8859-1, as the validator expects), and its report. */
public record SpedFiscalFile(String fileName, byte[] content, SpedValidationReport report) {
}
