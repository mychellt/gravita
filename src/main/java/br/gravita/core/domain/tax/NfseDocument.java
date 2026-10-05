package br.gravita.core.domain.tax;

import lombok.Builder;
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
 * or {@link #reject} returns it to DRAFT so it can be transmitted again. UC-M4-05 cancels an AUTHORIZED document
 * ({@link #cancel}): it moves to {@link NfseStatus#CANCELLED} carrying the justification, and is never deleted.
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
	/** Why and when the municipality confirmed the cancellation; {@code null} unless {@link NfseStatus#CANCELLED}. */
	private final String cancellationJustification;
	private final Instant cancelledAt;

	/** Reconstructs a document from persistence, at any status in its lifecycle: {@code NfseDocument.builder()...build()}. */
	@Builder
	public NfseDocument(final NfseId id, final NfseStatus status, final CompanyId providerCompanyId,
			final String providerMunicipalityIbgeCode, final NfseTomador tomador, final String serviceCode,
			final PlaceOfProvision placeOfProvision, final String issMunicipalityIbgeCode, final BigDecimal serviceAmount,
			final BigDecimal issRate, final BigDecimal issAmount, final String issRateOverrideJustification,
			final List<NfseWithholding> withholdings, final String discrimination, final String rpsSeries, final Long rpsNumber,
			final Instant createdAt, final String nfseSeries, final Long nfseNumber, final Instant draftAt, final Instant sentAt, final String protocol,
			final Instant authorizedAt, final String xmlReference, final String lastRejectionReason, final String cancellationJustification,
			final Instant cancelledAt) {
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
		if (status == NfseStatus.AUTHORIZED || status == NfseStatus.CANCELLED) {
			requireText(protocol, "protocol");
			Objects.requireNonNull(authorizedAt, "authorizedAt is required");
			requireText(xmlReference, "xmlReference");
		}
		this.sentAt = sentAt;
		this.protocol = protocol;
		this.authorizedAt = authorizedAt;
		this.xmlReference = xmlReference;
		this.lastRejectionReason = lastRejectionReason;
		if (status == NfseStatus.CANCELLED) {
			requireText(cancellationJustification, "cancellationJustification");
			Objects.requireNonNull(cancelledAt, "cancelledAt is required");
		}
		this.cancellationJustification = cancellationJustification;
		this.cancelledAt = cancelledAt;
	}

	/**
	 * Issues an RPS. Enforces the aggregate's own invariants: a full tomador address whenever something is withheld at
	 * source, and a justification whenever the ISS rate was manually overridden.
	 */
	@Builder(builderMethodName = "issueRps", builderClassName = "IssueRpsBuilder")
	private NfseDocument(final NfseId id, final CompanyId providerCompanyId, final String providerMunicipalityIbgeCode,
			final NfseTomador tomador, final ServiceCode serviceCode, final PlaceOfProvision placeOfProvision,
			final String issMunicipalityIbgeCode, final BigDecimal serviceAmount, final BigDecimal issRate, final BigDecimal issAmount,
			final String issRateOverrideJustification, final List<NfseWithholding> withholdings, final String discrimination,
			final String rpsSeries, final Long rpsNumber, final Instant createdAt) {
		this(id, NfseStatus.RPS, providerCompanyId, providerMunicipalityIbgeCode,
				tomadorChecked(tomador, withholdings), serviceCode.value(), placeOfProvision,
				issMunicipalityIbgeCode, serviceAmount, issRate, issAmount,
				issRateOverrideJustification, withholdings, discrimination, rpsSeries, rpsNumber,
				createdAt, null, null, null, null, null, null, null, null, null, null);
	}

	/**
	 * Converts an RPS into an NFSe: assigns its series/number and moves it to {@link NfseStatus#DRAFT}, still awaiting
	 * transmission. Only an RPS can be converted; callers treat an already converted document as done.
	 */
	public NfseDocument convertToNfse(final String nfseSeries, final Long nfseNumber, final Instant draftAt) {
		if (!isRps()) {
			throw new BusinessRuleException("Only an RPS can be converted to an NFSe, but the document is " + status);
		}
		return new NfseDocument(id, NfseStatus.DRAFT, providerCompanyId, providerMunicipalityIbgeCode, tomador,
				serviceCode, placeOfProvision, issMunicipalityIbgeCode, serviceAmount, issRate, issAmount,
				issRateOverrideJustification, withholdings, discrimination, rpsSeries, rpsNumber, createdAt,
				nfseSeries, nfseNumber, draftAt, null, null, null, null, null, null, null);
	}

	/**
	 * Starts a transmission attempt: {@link NfseStatus#DRAFT} to {@link NfseStatus#SENT}. Only a DRAFT is
	 * transmittable - an RPS has no NFSe number yet and an authorized or cancelled document is already decided.
	 */
	public NfseDocument send(final Instant sentAt) {
		if (status != NfseStatus.DRAFT) {
			throw new BusinessRuleException("Only a DRAFT NFSe can be transmitted, but the document is " + status);
		}
		return withLifecycle(NfseStatus.SENT, sentAt, null, null, null, null, null, null);
	}

	/** The municipality accepted the NFSe: {@link NfseStatus#SENT} to {@link NfseStatus#AUTHORIZED}. */
	public NfseDocument authorize(final String protocol, final Instant authorizedAt, final String xmlReference) {
		requireSent("authorized");
		return withLifecycle(NfseStatus.AUTHORIZED, sentAt, protocol, authorizedAt, xmlReference, null, null, null);
	}

	/**
	 * The municipality refused the NFSe. The document goes back to {@link NfseStatus#DRAFT} - never left in SENT - so
	 * it can be corrected and transmitted again; the reason stays on it until the next attempt.
	 */
	public NfseDocument reject(final String reason) {
		requireSent("rejected");
		return withLifecycle(NfseStatus.DRAFT, sentAt, null, null, null, requireText(reason, "reason"), null, null);
	}

	/**
	 * The municipality confirmed the cancellation: {@link NfseStatus#AUTHORIZED} to {@link NfseStatus#CANCELLED}. The
	 * justification is mandatory and kept with the document, which retains its protocol and XML reference - a
	 * cancelled NFSe is still a fiscal record and is never deleted.
	 */
	public NfseDocument cancel(final String justification, final Instant cancelledAt) {
		if (status != NfseStatus.AUTHORIZED) {
			throw new BusinessRuleException("Only an AUTHORIZED NFSe can be cancelled, but the document is " + status);
		}
		return withLifecycle(NfseStatus.CANCELLED, sentAt, protocol, authorizedAt, xmlReference, null,
				requireText(justification, "justification"), cancelledAt);
	}

	private void requireSent(final String outcome) {
		if (status != NfseStatus.SENT) {
			throw new BusinessRuleException("Only a SENT NFSe can be " + outcome + ", but the document is " + status);
		}
	}

	private NfseDocument withLifecycle(final NfseStatus newStatus, final Instant sentAt, final String protocol, final Instant authorizedAt,
			final String xmlReference, final String lastRejectionReason, final String cancellationJustification, final Instant cancelledAt) {
		return new NfseDocument(id, newStatus, providerCompanyId, providerMunicipalityIbgeCode, tomador, serviceCode,
				placeOfProvision, issMunicipalityIbgeCode, serviceAmount, issRate, issAmount,
				issRateOverrideJustification, withholdings, discrimination, rpsSeries, rpsNumber, createdAt,
				nfseSeries, nfseNumber, draftAt, sentAt, protocol, authorizedAt, xmlReference, lastRejectionReason,
				cancellationJustification, cancelledAt);
	}

	public boolean isRps() {
		return status == NfseStatus.RPS;
	}

	/**
	 * Doc §5.2: a tomador needs a full address (and municipality) when any tax is withheld at source. Public so the
	 * issuing use case can check it before consuming an RPS number.
	 */
	/** The tomador, after checking that a full address is present whenever something is withheld at source. */
	private static NfseTomador tomadorChecked(final NfseTomador tomador, final List<NfseWithholding> withholdings) {
		requireFullAddressIfWithheld(tomador, withholdings);
		return tomador;
	}

	public static void requireFullAddressIfWithheld(final NfseTomador tomador, final List<NfseWithholding> withholdings) {
		if (withholdings != null && !withholdings.isEmpty() && !tomador.hasFullAddress()) {
			throw new BusinessRuleException(
					"Tomador requires a full address (including the municipality) when a tax is withheld at source");
		}
	}

	public RpsId getRpsId() {
		return RpsId.of(id.value());
	}

	public boolean isIssRateOverridden() {
		return issRateOverrideJustification != null;
	}

	private static String requireIbgeCode(final String value, final String field) {
		if (value == null || !IBGE_CODE.matcher(value).matches()) {
			throw new BusinessRuleException(field + " must have 7 digits");
		}
		return value;
	}

	private static String requireText(final String value, final String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(field + " is required");
		}
		return value;
	}

	private static BigDecimal requirePositive(final BigDecimal value, final String field) {
		if (value == null || value.signum() <= 0) {
			throw new BusinessRuleException(field + " must be greater than zero");
		}
		return value;
	}
}
