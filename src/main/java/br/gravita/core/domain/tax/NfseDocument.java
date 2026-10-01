package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * NFSe aggregate root. UC-M4-02 only creates it in its pre-conversion {@link NfseStatus#RPS} state, carrying the RPS
 * identity ({@code rpsSeries}/{@code rpsNumber}) and the resolved ISS/withholding data; the later lifecycle
 * (conversion, transmission, cancellation) is added by UC-M4-03..05.
 */
@Getter
public final class NfseDocument {

	private static final Pattern IBGE_CODE = Pattern.compile("\\d{7}");

	private final NfseId id;
	private final NfseStatus status;
	private final CompanyId providerCompanyId;
	private final String providerMunicipalityIbgeCode;
	private final NfseTomador tomador;
	private final String serviceCode;
	private final PlaceOfProvision placeOfProvision;
	/** Municipality ISS is due to, as decided by {@code placeOfProvision}. */
	private final String issMunicipalityIbgeCode;
	private final BigDecimal serviceAmount;
	private final BigDecimal issRate;
	private final BigDecimal issAmount;
	private final String issRateOverrideJustification;
	private final List<NfseWithholding> withholdings;
	private final String discrimination;
	private final String rpsSeries;
	private final Long rpsNumber;
	private final Instant createdAt;

	private NfseDocument(NfseId id, NfseStatus status, CompanyId providerCompanyId,
			String providerMunicipalityIbgeCode, NfseTomador tomador, String serviceCode,
			PlaceOfProvision placeOfProvision, String issMunicipalityIbgeCode, BigDecimal serviceAmount,
			BigDecimal issRate, BigDecimal issAmount, String issRateOverrideJustification,
			List<NfseWithholding> withholdings, String discrimination, String rpsSeries, Long rpsNumber,
			Instant createdAt) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.status = Objects.requireNonNull(status, "status is required");
		this.providerCompanyId = Objects.requireNonNull(providerCompanyId, "providerCompanyId is required");
		this.providerMunicipalityIbgeCode = requireIbgeCode(providerMunicipalityIbgeCode, "providerMunicipalityIbgeCode");
		this.tomador = Objects.requireNonNull(tomador, "tomador is required");
		this.serviceCode = requireText(serviceCode, "serviceCode");
		this.placeOfProvision = Objects.requireNonNull(placeOfProvision, "placeOfProvision is required");
		this.issMunicipalityIbgeCode = requireIbgeCode(issMunicipalityIbgeCode, "issMunicipalityIbgeCode");
		this.serviceAmount = requirePositive(serviceAmount, "serviceAmount");
		this.issRate = Objects.requireNonNull(issRate, "issRate is required");
		this.issAmount = Objects.requireNonNull(issAmount, "issAmount is required");
		this.issRateOverrideJustification = issRateOverrideJustification;
		this.withholdings = withholdings == null ? List.of() : List.copyOf(withholdings);
		this.discrimination = requireText(discrimination, "discrimination");
		this.rpsSeries = requireText(rpsSeries, "rpsSeries");
		this.rpsNumber = Objects.requireNonNull(rpsNumber, "rpsNumber is required");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
	}

	/**
	 * Issues an RPS. Enforces the aggregate's own invariants: a full tomador address whenever something is withheld at
	 * source, and a justification whenever the ISS rate was manually overridden.
	 */
	public static NfseDocument issueRps(NfseId id, CompanyId providerCompanyId, String providerMunicipalityIbgeCode,
			NfseTomador tomador, ServiceCode serviceCode, PlaceOfProvision placeOfProvision,
			String issMunicipalityIbgeCode, BigDecimal serviceAmount, BigDecimal issRate, BigDecimal issAmount,
			String issRateOverrideJustification, List<NfseWithholding> withholdings, String discrimination,
			String rpsSeries, Long rpsNumber, Instant createdAt) {
		requireFullAddressIfWithheld(tomador, withholdings);
		return new NfseDocument(id, NfseStatus.RPS, providerCompanyId, providerMunicipalityIbgeCode, tomador,
				serviceCode.value(), placeOfProvision, issMunicipalityIbgeCode, serviceAmount, issRate, issAmount,
				issRateOverrideJustification, withholdings, discrimination, rpsSeries, rpsNumber, createdAt);
	}

	/**
	 * Doc §5.2: a tomador needs a full address (and municipality) when any tax is withheld at source. Public so the
	 * issuing use case can check it before consuming an RPS number.
	 */
	public static void requireFullAddressIfWithheld(NfseTomador tomador, List<NfseWithholding> withholdings) {
		if (withholdings != null && !withholdings.isEmpty() && !tomador.hasFullAddress()) {
			throw new BusinessRuleException(
					"Tomador requires a full address (including the municipality) when a tax is withheld at source");
		}
	}

	/** Reconstructs a document from persistence, at any status in its lifecycle. */
	public static NfseDocument of(NfseId id, NfseStatus status, CompanyId providerCompanyId,
			String providerMunicipalityIbgeCode, NfseTomador tomador, String serviceCode,
			PlaceOfProvision placeOfProvision, String issMunicipalityIbgeCode, BigDecimal serviceAmount,
			BigDecimal issRate, BigDecimal issAmount, String issRateOverrideJustification,
			List<NfseWithholding> withholdings, String discrimination, String rpsSeries, Long rpsNumber,
			Instant createdAt) {
		return new NfseDocument(id, status, providerCompanyId, providerMunicipalityIbgeCode, tomador, serviceCode,
				placeOfProvision, issMunicipalityIbgeCode, serviceAmount, issRate, issAmount,
				issRateOverrideJustification, withholdings, discrimination, rpsSeries, rpsNumber, createdAt);
	}

	public RpsId getRpsId() {
		return RpsId.of(id.value());
	}

	public boolean isIssRateOverridden() {
		return issRateOverrideJustification != null;
	}

	private static String requireIbgeCode(String value, String field) {
		if (value == null || !IBGE_CODE.matcher(value).matches()) {
			throw new BusinessRuleException(field + " must have 7 digits");
		}
		return value;
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(field + " is required");
		}
		return value;
	}

	private static BigDecimal requirePositive(BigDecimal value, String field) {
		if (value == null || value.signum() <= 0) {
			throw new BusinessRuleException(field + " must be greater than zero");
		}
		return value;
	}
}
