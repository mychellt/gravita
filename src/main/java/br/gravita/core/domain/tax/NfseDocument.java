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
 * NFSe aggregate root. UC-M4-02 creates it in its pre-conversion {@link NfseStatus#RPS} state, carrying the RPS
 * identity ({@code rpsSeries}/{@code rpsNumber}) and the resolved ISS/withholding data; UC-M4-03 converts it into a
 * {@link NfseStatus#DRAFT} NFSe by assigning the NFSe {@code nfseSeries}/{@code nfseNumber} (scoped per company and
 * municipality, independent of the RPS and NFe series). UC-M4-04 transmits a DRAFT: {@link #send} moves it to
 * {@link NfseStatus#SENT} for the duration of the attempt, then {@link #authorize} records the municipality's protocol
 * or {@link #reject} returns it to DRAFT so it can be transmitted again. Cancellation is added by UC-M4-05.
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
	/** NFSe series/number, assigned at conversion; {@code null} while the document is still an RPS. */
	private final String nfseSeries;
	private final Long nfseNumber;
	/** When the document became a {@link NfseStatus#DRAFT}; {@code null} while it is still an RPS. */
	private final Instant draftAt;
	/** When transmission last started; {@code null} until the first attempt. */
	private final Instant sentAt;
	/** Municipality protocol and authorization time; {@code null} until {@link NfseStatus#AUTHORIZED}. */
	private final String protocol;
	private final Instant authorizedAt;
	/** Reference to the authorized XML in {@code XmlObjectStoragePort}; the document never holds the XML itself. */
	private final String xmlReference;
	/** Why the municipality refused the latest attempt; kept while the document is back in DRAFT, cleared on resend. */
	private final String lastRejectionReason;

	private NfseDocument(NfseId id, NfseStatus status, CompanyId providerCompanyId,
			String providerMunicipalityIbgeCode, NfseTomador tomador, String serviceCode,
			PlaceOfProvision placeOfProvision, String issMunicipalityIbgeCode, BigDecimal serviceAmount,
			BigDecimal issRate, BigDecimal issAmount, String issRateOverrideJustification,
			List<NfseWithholding> withholdings, String discrimination, String rpsSeries, Long rpsNumber,
			Instant createdAt, String nfseSeries, Long nfseNumber, Instant draftAt, Instant sentAt, String protocol,
			Instant authorizedAt, String xmlReference, String lastRejectionReason) {
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
		if (status != NfseStatus.RPS) {
			requireText(nfseSeries, "nfseSeries");
			Objects.requireNonNull(nfseNumber, "nfseNumber is required");
			Objects.requireNonNull(draftAt, "draftAt is required");
		}
		this.nfseSeries = nfseSeries;
		this.nfseNumber = nfseNumber;
		this.draftAt = draftAt;
		if (status == NfseStatus.AUTHORIZED) {
			requireText(protocol, "protocol");
			Objects.requireNonNull(authorizedAt, "authorizedAt is required");
			requireText(xmlReference, "xmlReference");
		}
		this.sentAt = sentAt;
		this.protocol = protocol;
		this.authorizedAt = authorizedAt;
		this.xmlReference = xmlReference;
		this.lastRejectionReason = lastRejectionReason;
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
				issRateOverrideJustification, withholdings, discrimination, rpsSeries, rpsNumber, createdAt, null, null,
				null, null, null, null, null, null);
	}

	/**
	 * Converts an RPS into an NFSe: assigns its series/number and moves it to {@link NfseStatus#DRAFT}, still awaiting
	 * transmission. Only an RPS can be converted; callers treat an already converted document as done.
	 */
	public NfseDocument convertToNfse(String nfseSeries, Long nfseNumber, Instant draftAt) {
		if (!isRps()) {
			throw new BusinessRuleException("Only an RPS can be converted to an NFSe, but the document is " + status);
		}
		return new NfseDocument(id, NfseStatus.DRAFT, providerCompanyId, providerMunicipalityIbgeCode, tomador,
				serviceCode, placeOfProvision, issMunicipalityIbgeCode, serviceAmount, issRate, issAmount,
				issRateOverrideJustification, withholdings, discrimination, rpsSeries, rpsNumber, createdAt,
				nfseSeries, nfseNumber, draftAt, null, null, null, null, null);
	}

	/**
	 * Starts a transmission attempt: {@link NfseStatus#DRAFT} to {@link NfseStatus#SENT}. Only a DRAFT is
	 * transmittable - an RPS has no NFSe number yet and an authorized or cancelled document is already decided.
	 */
	public NfseDocument send(Instant sentAt) {
		if (status != NfseStatus.DRAFT) {
			throw new BusinessRuleException("Only a DRAFT NFSe can be transmitted, but the document is " + status);
		}
		return withLifecycle(NfseStatus.SENT, sentAt, null, null, null, null);
	}

	/** The municipality accepted the NFSe: {@link NfseStatus#SENT} to {@link NfseStatus#AUTHORIZED}. */
	public NfseDocument authorize(String protocol, Instant authorizedAt, String xmlReference) {
		requireSent("authorized");
		return withLifecycle(NfseStatus.AUTHORIZED, sentAt, protocol, authorizedAt, xmlReference, null);
	}

	/**
	 * The municipality refused the NFSe. The document goes back to {@link NfseStatus#DRAFT} - never left in SENT - so
	 * it can be corrected and transmitted again; the reason stays on it until the next attempt.
	 */
	public NfseDocument reject(String reason) {
		requireSent("rejected");
		return withLifecycle(NfseStatus.DRAFT, sentAt, null, null, null, requireText(reason, "reason"));
	}

	private void requireSent(String outcome) {
		if (status != NfseStatus.SENT) {
			throw new BusinessRuleException("Only a SENT NFSe can be " + outcome + ", but the document is " + status);
		}
	}

	private NfseDocument withLifecycle(NfseStatus newStatus, Instant sentAt, String protocol, Instant authorizedAt,
			String xmlReference, String lastRejectionReason) {
		return new NfseDocument(id, newStatus, providerCompanyId, providerMunicipalityIbgeCode, tomador, serviceCode,
				placeOfProvision, issMunicipalityIbgeCode, serviceAmount, issRate, issAmount,
				issRateOverrideJustification, withholdings, discrimination, rpsSeries, rpsNumber, createdAt,
				nfseSeries, nfseNumber, draftAt, sentAt, protocol, authorizedAt, xmlReference, lastRejectionReason);
	}

	public boolean isRps() {
		return status == NfseStatus.RPS;
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
			Instant createdAt, String nfseSeries, Long nfseNumber, Instant draftAt, Instant sentAt, String protocol,
			Instant authorizedAt, String xmlReference, String lastRejectionReason) {
		return new NfseDocument(id, status, providerCompanyId, providerMunicipalityIbgeCode, tomador, serviceCode,
				placeOfProvision, issMunicipalityIbgeCode, serviceAmount, issRate, issAmount,
				issRateOverrideJustification, withholdings, discrimination, rpsSeries, rpsNumber, createdAt,
				nfseSeries, nfseNumber, draftAt, sentAt, protocol, authorizedAt, xmlReference, lastRejectionReason);
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
