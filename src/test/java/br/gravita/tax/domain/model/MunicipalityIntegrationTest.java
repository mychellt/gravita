package br.gravita.tax.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.MunicipalityIntegrationId;
import br.gravita.core.domain.tax.NfseStandard;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MunicipalityIntegrationTest {

	private static MunicipalityIntegration create(NfseStandard standard, String version, String url,
			boolean homologated) {
		return MunicipalityIntegration.of(MunicipalityIntegrationId.of(UUID.randomUUID()), "3550308", standard,
				version, url, CertificateType.A1, List.of("inscricaoMunicipal"), homologated);
	}

	@ParameterizedTest
	@EnumSource(NfseStandard.class)
	@DisplayName("Accepts each supported standard")
	void ac1_acceptsEachSupportedStandard(NfseStandard standard) {
		MunicipalityIntegration integration = create(standard, "2.04", "https://nfse.example/ws", true);

		assertThat(integration.getStandard()).isEqualTo(standard);
		assertThat(integration.getRequiredFields()).containsExactly("inscricaoMunicipal");
	}

	@Test
	@DisplayName("Treats a non-homologated integration as valid even without version and URL")
	void ac2_nonHomologatedIsValidEvenWithoutVersionAndUrl() {
		MunicipalityIntegration integration = create(NfseStandard.ABRASF, null, null, false);

		assertThat(integration.isHomologated()).isFalse();
	}

	@Test
	@DisplayName("Requires a version and URL when homologated")
	void homologatedRequiresVersionAndUrl() {
		assertThatThrownBy(() -> create(NfseStandard.ABRASF, " ", "https://x", true))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("version");
		assertThatThrownBy(() -> create(NfseStandard.ABRASF, "2.04", null, true))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("webserviceUrl");
	}

	@Test
	@DisplayName("Rejects a malformed IBGE code")
	void rejectsMalformedIbgeCode() {
		assertThatThrownBy(() -> MunicipalityIntegration.of(MunicipalityIntegrationId.of(UUID.randomUUID()), "123",
				NfseStandard.BETHA, null, null, CertificateType.A1, null, false))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("ibgeCode");
	}

	@Test
	@DisplayName("Updating replaces the configuration while keeping identity")
	void updateReplacesConfigurationKeepingIdentity() {
		MunicipalityIntegration integration = create(NfseStandard.ABRASF, "2.04", "https://old", false);
		MunicipalityIntegrationId id = integration.getId();

		integration.update(NfseStandard.ISSNET, "1.0", "https://new", CertificateType.A3, null, true);

		assertThat(integration.getId()).isEqualTo(id);
		assertThat(integration.getIbgeCode()).isEqualTo("3550308");
		assertThat(integration.getStandard()).isEqualTo(NfseStandard.ISSNET);
		assertThat(integration.getWebserviceUrl()).isEqualTo("https://new");
		assertThat(integration.getRequiredCertificateType()).isEqualTo(CertificateType.A3);
		assertThat(integration.getRequiredFields()).isEmpty();
		assertThat(integration.isHomologated()).isTrue();
	}

	@Test
	@DisplayName("Leaves the configuration untouched when an update fails")
	void failedUpdateLeavesConfigurationUntouched() {
		MunicipalityIntegration integration = create(NfseStandard.ABRASF, "2.04", "https://old", true);

		assertThatThrownBy(() -> integration.update(NfseStandard.BETHA, null, null, CertificateType.A1, null, true))
				.isInstanceOf(BusinessRuleException.class);

		assertThat(integration.getStandard()).isEqualTo(NfseStandard.ABRASF);
		assertThat(integration.getWebserviceUrl()).isEqualTo("https://old");
	}
}
