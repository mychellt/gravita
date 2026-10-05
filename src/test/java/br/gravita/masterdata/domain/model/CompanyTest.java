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
		Company company = validCompanyBuilder().build();

		assertThat(company.getCnpj()).isEqualTo(VALID_CNPJ);
		assertThat(company.getIe()).isEqualTo("123456789");
		assertThat(company.getIm()).isEqualTo("987654");
		assertThat(company.getTaxRegime()).isEqualTo(TaxRegime.SIMPLES_NACIONAL);
		assertThat(company.getSefazEnvironment()).isEqualTo(SefazEnvironment.HOMOLOGATION);
		assertThat(company.getParentCompanyId()).isNull();
	}

	@Test
	@DisplayName("Allows registering a branch with a parent company id")
	void shouldAllowRegisteringBranchWithParentCompanyId() {
		CompanyId parentId = CompanyId.of(UUID.randomUUID());

		Company branch = validCompanyBuilder().parentCompanyId(parentId).build();

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
		Company company = build(b -> b.state("sp"));

		assertThat(company.getState()).isEqualTo("SP");
	}

	private Company build(java.util.function.Consumer<Builder> customize) {
		Builder builder = validCompanyBuilder();
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

		Builder name(String name) {
			this.name = name;
			return this;
		}

		Builder cnpj(Document cnpj) {
			this.cnpj = cnpj;
			return this;
		}

		Builder ie(String ie) {
			this.ie = ie;
			return this;
		}

		Builder im(String im) {
			this.im = im;
			return this;
		}

		Builder taxRegime(TaxRegime taxRegime) {
			this.taxRegime = taxRegime;
			return this;
		}

		Builder sefazEnvironment(SefazEnvironment sefazEnvironment) {
			this.sefazEnvironment = sefazEnvironment;
			return this;
		}

		Builder parentCompanyId(CompanyId parentCompanyId) {
			this.parentCompanyId = parentCompanyId;
			return this;
		}

		Builder state(String state) {
			this.state = state;
			return this;
		}

		Company build() {
			return Company.of(id, name, cnpj, ie, im, cnae, taxRegime, simplesOptante, sefazEnvironment, address, state,
					issuingEmail, phone, logoUrl, parentCompanyId);
		}
	}
}
