package br.gravita.tax.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.NfseWithholding;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.TomadorAddress;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NfseDocumentTest {

	private static final String CNPJ = "11222333000181";
	private static final String CPF = "52998224725";

	private static TomadorAddress address() {
		return new TomadorAddress("Rua A", "10", null, "Centro", "01001000", "SP");
	}

	private static NfseTomador tomador(String ibge, TomadorAddress address) {
		return NfseTomador.of(null, CNPJ, PersonType.COMPANY, "Tomador SA", ibge, address);
	}

	private static NfseWithholding withholding() {
		return new NfseWithholding(TaxType.PIS, new BigDecimal("1000.00"), new BigDecimal("0.65"),
				new BigDecimal("6.50"));
	}

	private static NfseDocument issue(NfseTomador tomador, List<NfseWithholding> withholdings,
			BigDecimal serviceAmount, String discrimination) {
		return NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), CompanyId.of(UUID.randomUUID()), "3550308",
				tomador, ServiceCode.of("1.05"), PlaceOfProvision.PROVIDER, "3550308", serviceAmount,
				new BigDecimal("5.0000"), new BigDecimal("50.00"), null, withholdings, discrimination, "001", 7L,
				Instant.now());
	}

	@Test
	void issuesAnRpsInTheRpsStatusSharingItsIdWithTheDocument() {
		NfseDocument rps = issue(tomador("3550308", address()), List.of(withholding()), new BigDecimal("1000.00"),
				"Consultoria");

		assertThat(rps.getStatus()).isEqualTo(NfseStatus.RPS);
		assertThat(rps.getRpsId().value()).isEqualTo(rps.getId().value());
		assertThat(rps.getServiceCode()).isEqualTo("01.05");
		assertThat(rps.getRpsSeries()).isEqualTo("001");
		assertThat(rps.getRpsNumber()).isEqualTo(7L);
		assertThat(rps.isIssRateOverridden()).isFalse();
	}

	@Test
	void ac2_aWithheldTaxRequiresTheTomadorsFullAddress() {
		assertThatThrownBy(() -> issue(tomador("3550308", null), List.of(withholding()), new BigDecimal("1000.00"),
				"Consultoria")).isInstanceOf(BusinessRuleException.class).hasMessageContaining("full address");
		assertThatThrownBy(() -> issue(tomador(null, address()), List.of(withholding()), new BigDecimal("1000.00"),
				"Consultoria")).isInstanceOf(BusinessRuleException.class).hasMessageContaining("full address");
	}

	@Test
	void ac2_withoutWithholdingsAnAddressIsNotRequired() {
		NfseTomador pf = NfseTomador.of(null, CPF, PersonType.INDIVIDUAL, "Pessoa Fisica", null, null);

		assertThat(issue(pf, List.of(), new BigDecimal("100.00"), "Aula").getWithholdings()).isEmpty();
	}

	@Test
	void incompleteAddressIsRejectedWhenBuilt() {
		assertThatThrownBy(() -> new TomadorAddress("Rua A", "10", null, " ", "01001000", "SP"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void rejectsNonPositiveAmountAndBlankDiscrimination() {
		NfseTomador tomador = tomador("3550308", address());

		assertThatThrownBy(() -> issue(tomador, List.of(), BigDecimal.ZERO, "x"))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> issue(tomador, List.of(), new BigDecimal("10"), " "))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void rejectsMalformedMunicipalityCodes() {
		assertThatThrownBy(() -> tomador("123", address())).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), CompanyId.of(UUID.randomUUID()),
				"12", tomador("3550308", address()), ServiceCode.of("1.05"), PlaceOfProvision.PROVIDER, "3550308",
				new BigDecimal("10"), BigDecimal.ONE, BigDecimal.ONE, null, List.of(), "x", "001", 1L, Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void rejectsAnInvalidTomadorDocument() {
		assertThatThrownBy(() -> NfseTomador.of(null, "11111111111", PersonType.INDIVIDUAL, "X", null, null))
				.isInstanceOf(RuntimeException.class);
	}
}
