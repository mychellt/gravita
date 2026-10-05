package br.gravita.core.domain.masterdata;

import java.util.UUID;

public class CompanyNotFoundException extends RuntimeException {

	public CompanyNotFoundException(final UUID companyId) {
		super("Company not found: " + companyId);
	}
}
