package br.gravita.adapters.inbound.controllers.masterdata.dtos;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.masterdata.RegisterCompanyCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Body of {@code PATCH /api/companies/{id}}. Only the name and the tax regime are mandatory: a company created at signup
 * starts with an incomplete fiscal profile (no IE, IM, address, state…) and must be saveable while it is being filled in.
 * Whatever is informed is still validated by the domain.
 */
public record UpdateCompanyRequest(
		@NotBlank String name,
		String cnpj,
		String ie,
		String im,
		String cnae,
		@NotNull TaxRegime taxRegime,
		boolean simplesOptante,
		String address,
		String state,
		@Email String issuingEmail,
		String phone,
		String logoUrl,
		UUID parentCompanyId) {

	public RegisterCompanyCommand toCommand(final CompanyId id) {
		return new RegisterCompanyCommand(
				id,
				name,
				cnpj == null || cnpj.isBlank() ? null : Document.cnpj(cnpj),
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
