package br.gravita.core.domain.tax;

/** A series/number pair allocated to an NFSe, scoped per company and municipality. */
public record NfseNumber(String series, Long number) {
}
