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
import org.junit.jupiter.api.DisplayName;
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
	@DisplayName("Issues an RPS in the RPS status, sharing its id with the document")
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
	@DisplayName("Requires the tomador's full address when tax is withheld")
	void ac2_aWithheldTaxRequiresTheTomadorsFullAddress() {
		assertThatThrownBy(() -> issue(tomador("3550308", null), List.of(withholding()), new BigDecimal("1000.00"),
				"Consultoria")).isInstanceOf(BusinessRuleException.class).hasMessageContaining("full address");
		assertThatThrownBy(() -> issue(tomador(null, address()), List.of(withholding()), new BigDecimal("1000.00"),
				"Consultoria")).isInstanceOf(BusinessRuleException.class).hasMessageContaining("full address");
	}

	@Test
	@DisplayName("Does not require an address when nothing is withheld")
	void ac2_withoutWithholdingsAnAddressIsNotRequired() {
		NfseTomador pf = NfseTomador.of(null, CPF, PersonType.INDIVIDUAL, "Pessoa Fisica", null, null);

		assertThat(issue(pf, List.of(), new BigDecimal("100.00"), "Aula").getWithholdings()).isEmpty();
	}

	@Test
	@DisplayName("Rejects an incomplete address when the document is built")
	void incompleteAddressIsRejectedWhenBuilt() {
		assertThatThrownBy(() -> new TomadorAddress("Rua A", "10", null, " ", "01001000", "SP"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a non-positive amount and a blank discrimination")
	void rejectsNonPositiveAmountAndBlankDiscrimination() {
		NfseTomador tomador = tomador("3550308", address());

		assertThatThrownBy(() -> issue(tomador, List.of(), BigDecimal.ZERO, "x"))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> issue(tomador, List.of(), new BigDecimal("10"), " "))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects malformed municipality codes")
	void rejectsMalformedMunicipalityCodes() {
		assertThatThrownBy(() -> tomador("123", address())).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), CompanyId.of(UUID.randomUUID()),
				"12", tomador("3550308", address()), ServiceCode.of("1.05"), PlaceOfProvision.PROVIDER, "3550308",
				new BigDecimal("10"), BigDecimal.ONE, BigDecimal.ONE, null, List.of(), "x", "001", 1L, Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects an invalid tomador document")
	void rejectsAnInvalidTomadorDocument() {
		assertThatThrownBy(() -> NfseTomador.of(null, "11111111111", PersonType.INDIVIDUAL, "X", null, null))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	@DisplayName("Converts an RPS into a draft with its NFS-e series and number, keeping the RPS identity")
	void convertsAnRpsIntoADraftWithItsNfseSeriesAndNumberKeepingTheRpsIdentity() {
		NfseDocument rps = issue(tomador("3550308", address()), List.of(withholding()), new BigDecimal("1000.00"),
				"Consultoria");
		Instant at = Instant.parse("2026-10-01T12:00:00Z");

		NfseDocument draft = rps.convertToNfse("1", 15L, at);

		assertThat(rps.getStatus()).isEqualTo(NfseStatus.RPS);
		assertThat(rps.getNfseNumber()).isNull();
		assertThat(draft.getStatus()).isEqualTo(NfseStatus.DRAFT);
		assertThat(draft.getId()).isEqualTo(rps.getId());
		assertThat(draft.getNfseSeries()).isEqualTo("1");
		assertThat(draft.getNfseNumber()).isEqualTo(15L);
		assertThat(draft.getDraftAt()).isEqualTo(at);
		assertThat(draft.getRpsSeries()).isEqualTo("001");
		assertThat(draft.getRpsNumber()).isEqualTo(7L);
		assertThat(draft.getWithholdings()).isEqualTo(rps.getWithholdings());
	}

	@Test
	@DisplayName("Rejects converting a document that is not an RPS again")
	void aDocumentThatIsNotAnRpsCannotBeConvertedAgain() {
		NfseDocument draft = issue(tomador("3550308", address()), List.of(), new BigDecimal("1000.00"), "Consultoria")
				.convertToNfse("1", 1L, Instant.now());

		assertThat(draft.isRps()).isFalse();
		assertThatThrownBy(() -> draft.convertToNfse("1", 2L, Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Requires a converted document to have its number, series and timestamp")
	void aConvertedDocumentRequiresItsNumberSeriesAndTimestamp() {
		NfseDocument rps = issue(tomador("3550308", address()), List.of(), new BigDecimal("1000.00"), "Consultoria");

		assertThatThrownBy(() -> rps.convertToNfse(" ", 1L, Instant.now())).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> rps.convertToNfse("1", null, Instant.now()))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> rps.convertToNfse("1", 1L, null)).isInstanceOf(NullPointerException.class);
	}

	private static NfseDocument draft() {
		return issue(tomador(null, null), List.of(), new BigDecimal("1000.00"), "Consultoria")
				.convertToNfse("1", 9L, Instant.parse("2026-10-01T10:00:00Z"));
	}

	@Test
	@DisplayName("Moves a draft through sent to authorized, keeping only an XML reference")
	void transmissionMovesADraftThroughSentToAuthorizedKeepingOnlyAnXmlReference() {
		Instant sentAt = Instant.parse("2026-10-01T11:00:00Z");
		Instant authorizedAt = Instant.parse("2026-10-01T11:00:02Z");

		NfseDocument sent = draft().send(sentAt);
		NfseDocument authorized = sent.authorize("PROT-1", authorizedAt, "xml/ref-1");

		assertThat(sent.getStatus()).isEqualTo(NfseStatus.SENT);
		assertThat(sent.getSentAt()).isEqualTo(sentAt);
		assertThat(authorized.getStatus()).isEqualTo(NfseStatus.AUTHORIZED);
		assertThat(authorized.getProtocol()).isEqualTo("PROT-1");
		assertThat(authorized.getAuthorizedAt()).isEqualTo(authorizedAt);
		assertThat(authorized.getXmlReference()).isEqualTo("xml/ref-1");
		assertThat(authorized.getSentAt()).isEqualTo(sentAt);
		assertThat(authorized.getNfseNumber()).isEqualTo(9L);
	}

	@Test
	@DisplayName("Returns a rejected attempt to draft with the reason so it can be sent again")
	void aRejectedAttemptReturnsToDraftWithTheReasonAndCanBeSentAgain() {
		NfseDocument rejected = draft().send(Instant.now()).reject("Item de servico invalido");

		assertThat(rejected.getStatus()).isEqualTo(NfseStatus.DRAFT);
		assertThat(rejected.getLastRejectionReason()).isEqualTo("Item de servico invalido");
		assertThat(rejected.getProtocol()).isNull();

		NfseDocument retried = rejected.send(Instant.now());
		assertThat(retried.getStatus()).isEqualTo(NfseStatus.SENT);
		assertThat(retried.getLastRejectionReason()).isNull();
	}

	@Test
	@DisplayName("Only a draft can be sent and only a sent document can be decided")
	void onlyADraftCanBeSentAndOnlyASentOneCanBeDecided() {
		NfseDocument rps = issue(tomador(null, null), List.of(), new BigDecimal("1000.00"), "Consultoria");
		NfseDocument draft = draft();
		NfseDocument authorized = draft.send(Instant.now()).authorize("P", Instant.now(), "ref");

		assertThatThrownBy(() -> rps.send(Instant.now())).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> authorized.send(Instant.now())).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> draft.authorize("P", Instant.now(), "ref"))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> draft.reject("no")).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Requires an authorized document to have its protocol, timestamp and XML reference")
	void anAuthorizedDocumentRequiresItsProtocolTimestampAndXmlReference() {
		NfseDocument sent = draft().send(Instant.now());

		assertThatThrownBy(() -> sent.authorize(" ", Instant.now(), "ref")).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> sent.authorize("P", null, "ref")).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> sent.authorize("P", Instant.now(), null)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Cancelling an authorized document keeps its fiscal data and records the justification")
	void cancellingAnAuthorizedDocumentKeepsItsFiscalDataAndRecordsTheJustification() {
		Instant cancelledAt = Instant.parse("2026-10-02T09:00:00Z");
		NfseDocument authorized = draft().send(Instant.now()).authorize("PROT-1", Instant.now(), "xml/ref-1");

		NfseDocument cancelled = authorized.cancel("Servico nao prestado", cancelledAt);

		assertThat(cancelled.getStatus()).isEqualTo(NfseStatus.CANCELLED);
		assertThat(cancelled.getId()).isEqualTo(authorized.getId());
		assertThat(cancelled.getCancellationJustification()).isEqualTo("Servico nao prestado");
		assertThat(cancelled.getCancelledAt()).isEqualTo(cancelledAt);
		assertThat(cancelled.getProtocol()).isEqualTo("PROT-1");
		assertThat(cancelled.getXmlReference()).isEqualTo("xml/ref-1");
		assertThat(cancelled.getNfseNumber()).isEqualTo(authorized.getNfseNumber());
		assertThat(authorized.getStatus()).isEqualTo(NfseStatus.AUTHORIZED);
	}

	@Test
	@DisplayName("Only an authorized document can be cancelled, and only once")
	void onlyAnAuthorizedDocumentCanBeCancelledAndOnlyOnce() {
		NfseDocument rps = issue(tomador(null, null), List.of(), new BigDecimal("1000.00"), "Consultoria");
		NfseDocument draft = draft();
		NfseDocument sent = draft.send(Instant.now());
		NfseDocument cancelled = sent.authorize("P", Instant.now(), "ref").cancel("motivo", Instant.now());

		for (NfseDocument document : List.of(rps, draft, sent, cancelled)) {
			assertThatThrownBy(() -> document.cancel("motivo", Instant.now()))
					.isInstanceOf(BusinessRuleException.class).hasMessageContaining(document.getStatus().name());
		}
	}

	@Test
	@DisplayName("Requires a justification and a timestamp to cancel")
	void cancellationRequiresAJustificationAndATimestamp() {
		NfseDocument authorized = draft().send(Instant.now()).authorize("P", Instant.now(), "ref");

		assertThatThrownBy(() -> authorized.cancel(null, Instant.now())).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> authorized.cancel("  ", Instant.now())).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> authorized.cancel("motivo", null)).isInstanceOf(NullPointerException.class);
	}
}
