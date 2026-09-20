package br.gravita.core.domain.system;

/**
 * Mirrors M1's per-company {@code SefazEnvironment} switch (production/homologation), so a
 * SEFAZ credential can be configured for one without disturbing the other.
 */
public enum IntegrationEnvironment {
	PRODUCTION,
	HOMOLOGATION
}
