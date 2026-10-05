package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;

public record RegisterCompanyCommand(
		CompanyId id,
		String name,
		Document cnpj,
		String ie,
		String im,
		String cnae,
		TaxRegime taxRegime,
		boolean simplesOptante,
		String address,
		String state,
		String issuingEmail,
		String phone,
		String logoUrl,
		CompanyId parentCompanyId) {
}
