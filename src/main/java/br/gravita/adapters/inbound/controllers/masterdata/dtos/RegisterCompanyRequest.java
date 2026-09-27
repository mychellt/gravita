package br.gravita.adapters.inbound.controllers.masterdata.dtos;

import br.gravita.core.ports.inbound.masterdata.RegisterCompanyCommand;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterCompanyRequest(
		@NotBlank String cnpj,
		@NotBlank String ie,
		@NotBlank String im,
		@NotBlank String cnae,
		@NotNull TaxRegime taxRegime,
		boolean simplesOptante,
		@NotBlank String address,
		@NotBlank String state,
		@NotBlank @Email String issuingEmail,
		@NotBlank String phone,
		String logoUrl,
		UUID parentCompanyId) {

	public RegisterCompanyCommand toCommand(CompanyId id) {
		return new RegisterCompanyCommand(
				id,
				Document.cnpj(cnpj),
				ie,
				im,
				cnae,
				taxRegime,
				simplesOptante,
				address,
				state,
				issuingEmail,
				phone,
				logoUrl,
				parentCompanyId == null ? null : CompanyId.of(parentCompanyId));
	}
}
