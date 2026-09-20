package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;

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
