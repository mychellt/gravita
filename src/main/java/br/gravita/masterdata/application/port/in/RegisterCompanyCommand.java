package br.gravita.masterdata.application.port.in;

import br.gravita.masterdata.domain.model.CompanyId;
import br.gravita.masterdata.domain.model.TaxRegime;
import br.gravita.shared.Document;

/**
 * {@code id} is null for a new registration (POST) and set for the update
 * path (PATCH); both are served by {@link RegisterCompanyUseCase}.
 */
public record RegisterCompanyCommand(
		CompanyId id,
		Document cnpj,
		String ie,
		String im,
		String cnae,
		TaxRegime taxRegime,
		boolean simplesOptante,
		String address,
		String issuingEmail,
		String phone,
		String logoUrl,
		CompanyId parentCompanyId) {
}
