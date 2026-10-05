package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public final class Company {

	private static final Pattern IE_DIGITS = Pattern.compile("\\d{2,14}");
	private static final Pattern IM_DIGITS = Pattern.compile("\\d{1,15}");
	private static final Pattern UF_CODE = Pattern.compile("[A-Z]{2}");
	private static final String ISENTO = "ISENTO";

	private final CompanyId id;
	private final String name;
	private final Document cnpj;
	private final String ie;
	private final String im;
	private final String cnae;
	private final TaxRegime taxRegime;
	private final boolean simplesOptante;
	private final SefazEnvironment sefazEnvironment;
	private final String address;
	private final String state;
	private final String issuingEmail;
	private final String phone;
	private final String logoUrl;
	private final CompanyId parentCompanyId;

	public Company(CompanyId id, String name, Document cnpj, String ie, String im, String cnae, TaxRegime taxRegime,
			boolean simplesOptante, SefazEnvironment sefazEnvironment, String address, String state,
			String issuingEmail, String phone, String logoUrl, CompanyId parentCompanyId) {
		this.id = id;
		this.name = requireName(name);
		this.cnpj = requireCnpj(cnpj);
		this.ie = validateIe(ie);
		this.im = validateIm(im);
		this.cnae = cnae;
		this.taxRegime = requireTaxRegime(taxRegime);
		this.simplesOptante = simplesOptante;
		this.sefazEnvironment = requireSefazEnvironment(sefazEnvironment);
		this.address = address;
		this.state = validateState(state);
		this.issuingEmail = issuingEmail;
		this.phone = phone;
		this.logoUrl = logoUrl;
		this.parentCompanyId = parentCompanyId;
	}

	public static Company of(CompanyId id, String name, Document cnpj, String ie, String im, String cnae, TaxRegime taxRegime,
			boolean simplesOptante, SefazEnvironment sefazEnvironment, String address, String state,
			String issuingEmail, String phone, String logoUrl, CompanyId parentCompanyId) {
		return new Company(id, name, cnpj, ie, im, cnae, taxRegime, simplesOptante, sefazEnvironment, address, state,
				issuingEmail, phone, logoUrl, parentCompanyId);
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new BusinessRuleException("Name is required");
		}
		return name.trim();
	}

	private static Document requireCnpj(Document cnpj) {
		if (cnpj == null) {
			throw new BusinessRuleException("CNPJ is required");
		}
		return cnpj;
	}

	private static TaxRegime requireTaxRegime(TaxRegime taxRegime) {
		if (taxRegime == null) {
			throw new BusinessRuleException("Tax regime is required");
		}
		return taxRegime;
	}

	private static SefazEnvironment requireSefazEnvironment(SefazEnvironment sefazEnvironment) {
		if (sefazEnvironment == null) {
			throw new BusinessRuleException("SEFAZ environment is required");
		}
		return sefazEnvironment;
	}

	private static String validateIe(String ie) {
		if (ie == null || ie.isBlank()) {
			throw new BusinessRuleException("IE is required");
		}
		String trimmed = ie.trim();
		if (!ISENTO.equalsIgnoreCase(trimmed) && !IE_DIGITS.matcher(trimmed).matches()) {
			throw new BusinessRuleException("Invalid IE: " + ie);
		}
		return trimmed;
	}

	private static String validateIm(String im) {
		if (im == null || im.isBlank()) {
			throw new BusinessRuleException("IM is required");
		}
		String trimmed = im.trim();
		if (!IM_DIGITS.matcher(trimmed).matches()) {
			throw new BusinessRuleException("Invalid IM: " + im);
		}
		return trimmed;
	}

	private static String validateState(String state) {
		if (state == null || state.isBlank()) {
			throw new BusinessRuleException("State (UF) is required");
		}
		String trimmed = state.trim().toUpperCase();
		if (!UF_CODE.matcher(trimmed).matches()) {
			throw new BusinessRuleException("Invalid state (UF): " + state);
		}
		return trimmed;
	}
}
