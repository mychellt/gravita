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
		this(id, name, cnpj, ie, im, cnae, taxRegime, simplesOptante, sefazEnvironment, address, state, issuingEmail,
				phone, logoUrl, parentCompanyId, false);
	}

	/** {@code lenient} skips the fiscal-profile rules (IE, IM, UF) for drafts and for rows read back from storage. */
	private Company(CompanyId id, String name, Document cnpj, String ie, String im, String cnae, TaxRegime taxRegime,
			boolean simplesOptante, SefazEnvironment sefazEnvironment, String address, String state,
			String issuingEmail, String phone, String logoUrl, CompanyId parentCompanyId, boolean lenient) {
		this.id = id;
		this.name = requireName(name);
		this.cnpj = requireCnpj(cnpj);
		this.ie = lenient && isBlank(ie) ? null : validateIe(ie);
		this.im = lenient && isBlank(im) ? null : validateIm(im);
		this.cnae = cnae;
		this.taxRegime = requireTaxRegime(taxRegime);
		this.simplesOptante = simplesOptante;
		this.sefazEnvironment = requireSefazEnvironment(sefazEnvironment);
		this.address = address;
		this.state = lenient && isBlank(state) ? null : validateState(state);
		this.issuingEmail = issuingEmail;
		this.phone = phone;
		this.logoUrl = logoUrl;
		this.parentCompanyId = parentCompanyId;
	}

	/**
	 * A company that only has what signup collects (name, CNPJ, phone). The fiscal profile (IE, IM, CNAE, address,
	 * UF, issuing e-mail) is filled in later through the regular update, which enforces the usual rules; until then
	 * it defaults to Simples Nacional and the homologation environment, so nothing is issued for real by accident.
	 */
	public static Company draft(CompanyId id, String name, Document cnpj, String phone) {
		return new Company(id, requireName(name), requireCnpj(cnpj), null, null, null, TaxRegime.SIMPLES_NACIONAL, false,
				SefazEnvironment.HOMOLOGATION, null, null, null, phone, null, null, true);
	}

	/** Rebuilds a stored company as it is, including a draft whose fiscal profile is still incomplete. */
	public static Company rehydrate(CompanyId id, String name, Document cnpj, String ie, String im, String cnae,
			TaxRegime taxRegime, boolean simplesOptante, SefazEnvironment sefazEnvironment, String address,
			String state, String issuingEmail, String phone, String logoUrl, CompanyId parentCompanyId) {
		return new Company(id, name, cnpj, ie, im, cnae, taxRegime, simplesOptante, sefazEnvironment, address, state,
				issuingEmail, phone, logoUrl, parentCompanyId, true);
	}

	/** True once the fiscal profile has everything the strict rules require. */
	public boolean isProfileComplete() {
		return ie != null && im != null && state != null;
	}

	public static Company of(CompanyId id, String name, Document cnpj, String ie, String im, String cnae, TaxRegime taxRegime,
			boolean simplesOptante, SefazEnvironment sefazEnvironment, String address, String state,
			String issuingEmail, String phone, String logoUrl, CompanyId parentCompanyId) {
		return new Company(id, name, cnpj, ie, im, cnae, taxRegime, simplesOptante, sefazEnvironment, address, state,
				issuingEmail, phone, logoUrl, parentCompanyId);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
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
