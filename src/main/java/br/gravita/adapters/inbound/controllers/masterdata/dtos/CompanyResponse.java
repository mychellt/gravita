package br.gravita.adapters.inbound.controllers.masterdata.dtos;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.TaxRegime;

import java.util.UUID;

public record CompanyResponse(
		UUID id,
		String name,
		String cnpj,
		String ie,
		String im,
		String cnae,
		TaxRegime taxRegime,
		boolean simplesOptante,
		String address,
		String state,
		String issuingEmail,
		String phone,
		String logoUrl) {

	public static CompanyResponse from(final Company company) {
		return new CompanyResponse(company.getId().value(), company.getName(), company.getCnpj().number(),
				company.getIe(), company.getIm(), company.getCnae(), company.getTaxRegime(),
				company.isSimplesOptante(), company.getAddress(), company.getState(), company.getIssuingEmail(),
				company.getPhone(), company.getLogoUrl());
	}
}
