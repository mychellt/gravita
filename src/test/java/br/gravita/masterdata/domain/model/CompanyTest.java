package br.gravita.masterdata.domain.model;

import br.gravita.shared.BusinessRuleException;
import br.gravita.shared.Document;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyTest {

	private static final Document VALID_CNPJ = Document.cnpj("11222333000181");

	@Test
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
	void shouldAllowRegisteringBranchWithParentCompanyId() {
		CompanyId parentId = CompanyId.of(UUID.randomUUID());

		Company branch = validCompanyBuilder().parentCompanyId(parentId).build();

		assertThat(branch.getParentCompanyId()).isEqualTo(parentId);
	}

	@Test
	void shouldRejectNullCnpj() {
		assertThatThrownBy(() -> build(b -> b.cnpj(null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CNPJ");
	}

	@Test
	void shouldRejectNullTaxRegime() {
		assertThatThrownBy(() -> build(b -> b.taxRegime(null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Tax regime");
	}

	@Test
	void shouldRejectNullSefazEnvironment() {
		assertThatThrownBy(() -> build(b -> b.sefazEnvironment(null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("SEFAZ environment");
	}

	@Test
	void shouldRejectBlankIe() {
		assertThatThrownBy(() -> build(b -> b.ie(" ")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("IE");
	}

	@Test
	void shouldRejectNonNumericIe() {
		assertThatThrownBy(() -> build(b -> b.ie("ABC123")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Invalid IE");
	}

	@Test
	void shouldAcceptIsentoIeCaseInsensitive() {
		assertThatCode(() -> build(b -> b.ie("isento"))).doesNotThrowAnyException();
	}

	@Test
	void shouldRejectBlankIm() {
		assertThatThrownBy(() -> build(b -> b.im("")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("IM");
	}

	@Test
	void shouldRejectNonNumericIm() {
		assertThatThrownBy(() -> build(b -> b.im("IM-1")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Invalid IM");
	}

	private void build(java.util.function.Consumer<Builder> customize) {
		Builder builder = validCompanyBuilder();
		customize.accept(builder);
		builder.build();
	}

	private Builder validCompanyBuilder() {
		return new Builder();
	}

	/** Small local builder to keep each test focused on a single overridden field. */
	private static final class Builder {
		private CompanyId id = CompanyId.of(UUID.randomUUID());
		private Document cnpj = VALID_CNPJ;
		private String ie = "123456789";
		private String im = "987654";
		private String cnae = "6201-5/01";
		private TaxRegime taxRegime = TaxRegime.SIMPLES_NACIONAL;
		private boolean simplesOptante = true;
		private SefazEnvironment sefazEnvironment = SefazEnvironment.HOMOLOGATION;
		private String address = "Rua Teste, 100";
		private String issuingEmail = "fiscal@empresa.com";
		private String phone = "11999999999";
		private String logoUrl = null;
		private CompanyId parentCompanyId = null;

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

		Company build() {
			return Company.of(id, cnpj, ie, im, cnae, taxRegime, simplesOptante, sefazEnvironment, address,
					issuingEmail, phone, logoUrl, parentCompanyId);
		}
	}
}
