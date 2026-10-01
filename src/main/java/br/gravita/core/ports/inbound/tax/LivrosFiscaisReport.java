package br.gravita.core.ports.inbound.tax;

/** The books of a period and their rendered files. {@code pdf} and {@code txt} carry the same books and totals. */
public record LivrosFiscaisReport(LivrosFiscaisBooks books, byte[] pdf, byte[] txt) {
}
