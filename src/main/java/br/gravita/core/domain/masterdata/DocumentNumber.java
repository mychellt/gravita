package br.gravita.core.domain.masterdata;

/**
 * A number reserved from a {@link DocumentSeries} (UC-15), unique for the
 * owning company and document type as long as it's paired with its series.
 */
public record DocumentNumber(String series, Long number) {
}
