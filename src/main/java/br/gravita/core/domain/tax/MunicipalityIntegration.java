package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CertificateType;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * NFSe integration configuration of one municipality, identified by its IBGE code. {@code homologated == false} is a
 * supported state: it only routes transmission to the guided manual-upload path.
 */
@Getter
public final class MunicipalityIntegration {

	private static final Pattern IBGE_CODE = Pattern.compile("\\d{7}");

	private final MunicipalityIntegrationId id;
	private final String ibgeCode;
	private NfseStandard standard;
	private String version;
	private String webserviceUrl;
	private CertificateType requiredCertificateType;
	private List<String> requiredFields;
	private boolean homologated;

	public MunicipalityIntegration(MunicipalityIntegrationId id, String ibgeCode, NfseStandard standard,
			String version, String webserviceUrl, CertificateType requiredCertificateType,
			List<String> requiredFields, boolean homologated) {
		this.id = Objects.requireNonNull(id, "id is required");
		if (ibgeCode == null || !IBGE_CODE.matcher(ibgeCode).matches()) {
			throw new BusinessRuleException("ibgeCode must have 7 digits");
		}
		this.ibgeCode = ibgeCode;
		apply(standard, version, webserviceUrl, requiredCertificateType, requiredFields, homologated);
	}

	public static MunicipalityIntegration of(MunicipalityIntegrationId id, String ibgeCode, NfseStandard standard,
			String version, String webserviceUrl, CertificateType requiredCertificateType,
			List<String> requiredFields, boolean homologated) {
		return new MunicipalityIntegration(id, ibgeCode, standard, version, webserviceUrl, requiredCertificateType,
				requiredFields, homologated);
	}

	/** Re-registration of the same IBGE code: replaces the configuration, keeping identity. */
	public void update(NfseStandard standard, String version, String webserviceUrl,
			CertificateType requiredCertificateType, List<String> requiredFields, boolean homologated) {
		apply(standard, version, webserviceUrl, requiredCertificateType, requiredFields, homologated);
	}

	private void apply(NfseStandard standard, String version, String webserviceUrl,
			CertificateType requiredCertificateType, List<String> requiredFields, boolean homologated) {
		if (standard == null) {
			throw new BusinessRuleException("standard is required");
		}
		if (requiredCertificateType == null) {
			throw new BusinessRuleException("requiredCertificateType is required");
		}
		if (homologated) {
			// Only a homologated integration is actually called, so only then are version and URL indispensable.
			requireNonBlank(version, "version");
			requireNonBlank(webserviceUrl, "webserviceUrl");
		}
		this.standard = standard;
		this.version = version;
		this.webserviceUrl = webserviceUrl;
		this.requiredCertificateType = requiredCertificateType;
		this.requiredFields = requiredFields == null ? List.of() : List.copyOf(requiredFields);
		this.homologated = homologated;
	}

	private static void requireNonBlank(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(field + " is required for a homologated integration");
		}
	}
}
