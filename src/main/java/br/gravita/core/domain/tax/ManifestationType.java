package br.gravita.core.domain.tax;

/**
 * UC-M2-07 (Manifestação do destinatário): the exact three manifestation
 * types SEFAZ accepts for an inbound NFe issued against this company by a
 * third party. Modeled as an enum rather than a validated string so "rejects
 * any other value" (AC1) is a compile-time guarantee, not a runtime check.
 */
public enum ManifestationType {
	CONFIRMED, UNKNOWN, OPERATION_NOT_PERFORMED
}
