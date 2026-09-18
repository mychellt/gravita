package br.gravita.masterdata.adapter.in.web.dto;

import br.gravita.masterdata.application.port.in.RegisterCompanyCommand;
import br.gravita.masterdata.domain.model.CompanyId;
import br.gravita.masterdata.domain.model.TaxRegime;
import br.gravita.shared.Document;
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
				issuingEmail,
				phone,
				logoUrl,
				parentCompanyId == null ? null : CompanyId.of(parentCompanyId));
	}
}
