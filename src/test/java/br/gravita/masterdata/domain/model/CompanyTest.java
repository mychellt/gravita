package br.gravita.masterdata.domain.model;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyTest {

	private static final Document VALID_CNPJ = Document.cnpj("11222333000181");

	@Test
	@DisplayName("Creates a company with valid data")
	void shouldCreateCompanyWithValidData() {
		final Company company = validCompanyBuilder().build();

		assertThat(company.getCnpj()).isEqualTo(VALID_CNPJ);
		assertThat(company.getIe()).isEqualTo("123456789");
		assertThat(company.getIm()).isEqualTo("987654");
		assertThat(company.getTaxRegime()).isEqualTo(TaxRegime.SIMPLES_NACIONAL);
		assertThat(company.getSefazEnvironment()).isEqualTo(SefazEnvironment.HOMOLOGATION);
		assertThat(company.getParentCompanyId()).isNull();
	}

	@Test
	@DisplayName("A signup draft holds only name, CNPJ and phone, and its fiscal profile is incomplete")
	void shouldCreateAnIncompleteDraft() {
		final Company draft = Company.draft(CompanyId.of(UUID.randomUUID()), " Acme Ltda ", VALID_CNPJ, "(11) 91234-5678");

		assertThat(draft.getName()).isEqualTo("Acme Ltda");
		assertThat(draft.getCnpj()).isEqualTo(VALID_CNPJ);
		assertThat(draft.getPhone()).isEqualTo("(11) 91234-5678");
		assertThat(draft.getIe()).isNull();
		assertThat(draft.getIm()).isNull();
		assertThat(draft.getState()).isNull();
		assertThat(draft.getTaxRegime()).isEqualTo(TaxRegime.SIMPLES_NACIONAL);
		assertThat(draft.getSefazEnvironment()).isEqualTo(SefazEnvironment.HOMOLOGATION);
		assertThat(draft.isProfileComplete()).isFalse();
		assertThat(validCompanyBuilder().build().isProfileComplete()).isTrue();
	}

	@Test
	@DisplayName("A draft still needs a name and a CNPJ")
	void shouldRequireNameAndCnpjForADraft() {
		final CompanyId id = CompanyId.of(UUID.randomUUID());

		assertThatThrownBy(() -> Company.draft(id, " ", VALID_CNPJ, null)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> Company.draft(id, "Acme", null, null)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("A stored draft can be read back, while the regular constructor keeps requiring the fiscal profile")
	void shouldRehydrateADraftButStayStrictOtherwise() {
		final CompanyId id = CompanyId.of(UUID.randomUUID());

		final Company stored = Company.rehydrate()
				.id(id)
				.name("Acme")
				.cnpj(VALID_CNPJ)
				.ie(null)
				.im(null)
				.cnae(null)
				.taxRegime(TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(false)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address(null)
				.state(null)
				.issuingEmail(null)
				.phone(null)
				.logoUrl(null)
				.parentCompanyId(null)
				.build();

		assertThat(stored.isProfileComplete()).isFalse();
		assertThatThrownBy(() -> Company.builder()
				.id(id)
				.name("Acme")
				.cnpj(VALID_CNPJ)
				.ie(null)
				.im("1")
				.cnae(null)
				.taxRegime(TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(false)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address(null)
				.state("SP")
				.issuingEmail(null)
				.phone(null)
				.logoUrl(null)
				.parentCompanyId(null)
				.build())
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("IE");
	}

	@Test
	@DisplayName("Allows registering a branch with a parent company id")
	void shouldAllowRegisteringBranchWithParentCompanyId() {
		final CompanyId parentId = CompanyId.of(UUID.randomUUID());

		final Company branch = validCompanyBuilder().parentCompanyId(parentId).build();

		assertThat(branch.getParentCompanyId()).isEqualTo(parentId);
	}

	@Test
	@DisplayName("Keeps the trimmed name")
	void shouldKeepTrimmedName() {
		assertThat(build(b -> b.name("  Acme Ltda  ")).getName()).isEqualTo("Acme Ltda");
	}

	@Test
	@DisplayName("Rejects a missing or blank name")
	void shouldRejectMissingName() {
		assertThatThrownBy(() -> build(b -> b.name(null))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Name");
		assertThatThrownBy(() -> build(b -> b.name("  "))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Name");
	}

	@Test
	@DisplayName("Rejects a null CNPJ")
	void shouldRejectNullCnpj() {
		assertThatThrownBy(() -> build(b -> b.cnpj(null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CNPJ");
	}

	@Test
	@DisplayName("Rejects a null tax regime")
	void shouldRejectNullTaxRegime() {
		assertThatThrownBy(() -> build(b -> b.taxRegime(null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Tax regime");
	}

	@Test
	@DisplayName("Rejects a null SEFAZ environment")
	void shouldRejectNullSefazEnvironment() {
		assertThatThrownBy(() -> build(b -> b.sefazEnvironment(null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("SEFAZ environment");
	}

	@Test
	@DisplayName("Rejects a blank state registration (IE)")
	void shouldRejectBlankIe() {
		assertThatThrownBy(() -> build(b -> b.ie(" ")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("IE");
	}

	@Test
	@DisplayName("Rejects a non-numeric state registration (IE)")
	void shouldRejectNonNumericIe() {
		assertThatThrownBy(() -> build(b -> b.ie("ABC123")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Invalid IE");
	}

	@Test
	@DisplayName("Accepts ISENTO as state registration (IE), case-insensitively")
	void shouldAcceptIsentoIeCaseInsensitive() {
		assertThatCode(() -> build(b -> b.ie("isento"))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Rejects a blank municipal registration (IM)")
	void shouldRejectBlankIm() {
		assertThatThrownBy(() -> build(b -> b.im("")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("IM");
	}

	@Test
	@DisplayName("Rejects a non-numeric municipal registration (IM)")
	void shouldRejectNonNumericIm() {
		assertThatThrownBy(() -> build(b -> b.im("IM-1")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Invalid IM");
	}

	@Test
	@DisplayName("Rejects a blank state")
	void shouldRejectBlankState() {
		assertThatThrownBy(() -> build(b -> b.state(" ")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("State");
	}

	@Test
	@DisplayName("Rejects an invalid state")
	void shouldRejectInvalidState() {
		assertThatThrownBy(() -> build(b -> b.state("SPX")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Invalid state");
	}

	@Test
	@DisplayName("Normalizes the state to uppercase")
	void shouldNormalizeStateToUppercase() {
		final Company company = build(b -> b.state("sp"));

		assertThat(company.getState()).isEqualTo("SP");
	}

	private Company build(final java.util.function.Consumer<Builder> customize) {
		final Builder builder = validCompanyBuilder();
		customize.accept(builder);
		return builder.build();
	}

	private Builder validCompanyBuilder() {
		return new Builder();
	}

	private static final class Builder {
		private CompanyId id = CompanyId.of(UUID.randomUUID());
		private String name = "Acme Ltda";
		private Document cnpj = VALID_CNPJ;
		private String ie = "123456789";
		private String im = "987654";
		private String cnae = "6201-5/01";
		private TaxRegime taxRegime = TaxRegime.SIMPLES_NACIONAL;
		private boolean simplesOptante = true;
		private SefazEnvironment sefazEnvironment = SefazEnvironment.HOMOLOGATION;
		private String address = "Rua Teste, 100";
		private String state = "SP";
		private String issuingEmail = "fiscal@empresa.com";
		private String phone = "11999999999";
		private String logoUrl = null;
		private CompanyId parentCompanyId = null;

		Builder name(final String name) {
			this.name = name;
			return this;
		}

		Builder cnpj(final Document cnpj) {
			this.cnpj = cnpj;
			return this;
		}

		Builder ie(final String ie) {
			this.ie = ie;
			return this;
		}

		Builder im(final String im) {
			this.im = im;
			return this;
		}

		Builder taxRegime(final TaxRegime taxRegime) {
			this.taxRegime = taxRegime;
			return this;
		}

		Builder sefazEnvironment(final SefazEnvironment sefazEnvironment) {
			this.sefazEnvironment = sefazEnvironment;
			return this;
		}

		Builder parentCompanyId(final CompanyId parentCompanyId) {
			this.parentCompanyId = parentCompanyId;
			return this;
		}

		Builder state(final String state) {
			this.state = state;
			return this;
		}

		Company build() {
			return Company.builder()
					.id(id)
					.name(name)
					.cnpj(cnpj)
					.ie(ie)
					.im(im)
					.cnae(cnae)
					.taxRegime(taxRegime)
					.simplesOptante(simplesOptante)
					.sefazEnvironment(sefazEnvironment)
					.address(address)
					.state(state)
					.issuingEmail(issuingEmail)
					.phone(phone)
					.logoUrl(logoUrl)
					.parentCompanyId(parentCompanyId)
					.build();
		}
	}
}
