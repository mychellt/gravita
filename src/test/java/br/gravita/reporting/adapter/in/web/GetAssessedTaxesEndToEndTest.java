package br.gravita.reporting.adapter.in.web;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Runs the real controller, service and read-model adapter over the real repositories; only session and permission are stubbed. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GetAssessedTaxesEndToEndTest {

	private static final ZoneId ZONE = ZoneId.systemDefault();
	private static final String MUNICIPALITY = "3304557";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NfeRepositoryPort nfeRepositoryPort;

	@Autowired
	private NfseRepositoryPort nfseRepositoryPort;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	@MockitoBean
	private CheckPermissionUseCase checkPermissionUseCase;

	private final CompanyId company = CompanyId.of(UUID.randomUUID());
	private long documentNumber = 900_000L;

	@BeforeEach
	void seed() {
		UserId caller = UserId.generate();
		when(sessionStorePort.resolve("valid-token")).thenReturn(Optional.of(caller));
		when(checkPermissionUseCase.execute(argThat(
				query -> query != null && query.userId().equals(caller) && query.module().equals("reporting")
						&& query.screen().equals("assessed-taxes")))).thenReturn(true);

		// Inside March 2017 (a month long past, so other tests' documents cannot collide), edges included.
		nfeRepositoryPort.save(nfe(NfeDocumentStatus.AUTHORIZED, at(3, 1, 0, 0), "100.00", "10.00", "1.65", "7.60"));
		nfeRepositoryPort.save(nfe(NfeDocumentStatus.AUTHORIZED, at(3, 31, 23, 30), "50.00", "5.00", "0.83", "3.80"));
		nfseRepositoryPort.save(nfse(true, at(3, 15, 9, 0), "30.00"));
		nfseRepositoryPort.save(nfse(true, at(3, 20, 9, 0), "12.50"));
		// Not authorized, or outside the month: none of these counts.
		nfeRepositoryPort.save(nfe(NfeDocumentStatus.REJECTED, null, "999.00", "999.00", "999.00", "999.00"));
		nfeRepositoryPort.save(nfe(NfeDocumentStatus.CANCELLED, at(3, 10, 12, 0), "999.00", "999.00", "999.00", "999.00"));
		nfeRepositoryPort.save(nfe(NfeDocumentStatus.AUTHORIZED, at(2, 28, 23, 30), "999.00", "999.00", "999.00", "999.00"));
		nfeRepositoryPort.save(nfe(NfeDocumentStatus.AUTHORIZED, at(4, 1, 0, 0), "999.00", "999.00", "999.00", "999.00"));
		nfseRepositoryPort.save(nfse(false, null, "999.00"));
		nfseRepositoryPort.save(nfse(true, at(4, 2, 9, 0), "999.00"));
	}

	@DisplayName("Sums the taxes of the period's authorized NF-e and NFS-e")
	@Test
	void sumsTheTaxesOfTheAuthorizedNfeAndNfseOfThePeriod() throws Exception {
		mockMvc.perform(get("/api/reports/assessed-taxes").param("period", "2017-03").header("Authorization",
				"Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.period").value("2017-03"))
				.andExpect(jsonPath("$.icms").value(150.0))
				.andExpect(jsonPath("$.ipi").value(15.0))
				.andExpect(jsonPath("$.pis").value(2.48))
				.andExpect(jsonPath("$.cofins").value(11.4))
				.andExpect(jsonPath("$.iss").value(42.5));
	}

	@DisplayName("Returns zero totals for a period without authorized documents")
	@Test
	void answersZeroTotalsForAPeriodWithoutAuthorizedDocuments() throws Exception {
		mockMvc.perform(get("/api/reports/assessed-taxes").param("period", "2017-01").header("Authorization",
				"Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.icms").value(0))
				.andExpect(jsonPath("$.ipi").value(0))
				.andExpect(jsonPath("$.pis").value(0))
				.andExpect(jsonPath("$.cofins").value(0))
				.andExpect(jsonPath("$.iss").value(0));
	}

	@DisplayName("Returns 400 when the period is missing or malformed")
	@Test
	void answersBadRequestWhenThePeriodIsMissingOrMalformed() throws Exception {
		mockMvc.perform(get("/api/reports/assessed-taxes").header("Authorization", "Bearer valid-token"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reports/assessed-taxes").param("period", "March").header("Authorization",
				"Bearer valid-token")).andExpect(status().isBadRequest());
	}

	@DisplayName("Returns 403 when the profile cannot view the tax summary")
	@Test
	void answersForbiddenWhenTheProfileCannotViewTheSummary() throws Exception {
		when(sessionStorePort.resolve("other-token")).thenReturn(Optional.of(UserId.generate()));

		mockMvc.perform(get("/api/reports/assessed-taxes").param("period", "2017-03").header("Authorization",
				"Bearer other-token")).andExpect(status().isForbidden());
	}

	@DisplayName("Returns 401 when there is no session")
	@Test
	void answersUnauthorizedWithoutASession() throws Exception {
		mockMvc.perform(get("/api/reports/assessed-taxes").param("period", "2017-03"))
				.andExpect(status().isUnauthorized());
	}

	private static Instant at(int month, int day, int hour, int minute) {
		return LocalDateTime.of(2017, month, day, hour, minute).atZone(ZONE).toInstant();
	}

	private NfeDocument nfe(NfeDocumentStatus status, Instant authorizedAt, String icms, String ipi, String pis,
			String cofins) {
		List<TaxLineBreakdown> lines = new ArrayList<>();
		lines.add(line(TaxType.ICMS, icms));
		lines.add(line(TaxType.IPI, ipi));
		lines.add(line(TaxType.PIS, pis));
		lines.add(line(TaxType.COFINS, cofins));
		// ICMS-ST is not one of the assessed taxes: it must not leak into the ICMS total.
		lines.add(line(TaxType.ICMS_ST, "777.00"));
		ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, "rice", lines);
		NfeItem item = new NfeItem(UUID.randomUUID(), "Arroz", BigDecimal.TEN, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		NfeRecipient recipient = NfeRecipient.of(PersonRef.of(UUID.randomUUID()), "11.222.333/0001-81",
				PersonType.COMPANY, "Cliente SA", "123456789", "RJ");
		long number = ++documentNumber;
		Instant cancelledAt = status == NfeDocumentStatus.CANCELLED ? authorizedAt.plusSeconds(3600) : null;
		return NfeDocument.of(NfeDocumentId.of(UUID.randomUUID()), company, null, NaturezaOperacao.VENDA,
				new Cfop("5102"), recipient, List.of(item), new BigDecimal("15.00"), BigDecimal.ZERO, BigDecimal.ZERO,
				null, null, null, TaxCalculationTotals.from(List.of(breakdown)), status, at(3, 1, 8, 0), "1", number,
				"35" + String.format("%042d", number), "protocol", false, null, null, null, List.of(), authorizedAt,
				cancelledAt == null ? null : "Cancelada a pedido", cancelledAt);
	}

	private static TaxLineBreakdown line(TaxType type, String amount) {
		return new TaxLineBreakdown(type, new BigDecimal("100.00"), new BigDecimal("10"), new BigDecimal(amount),
				new BigDecimal(amount), false, null);
	}

	private NfseDocument nfse(boolean authorized, Instant authorizedAt, String iss) {
		long rps = ++documentNumber;
		NfseDocument document = NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), company, MUNICIPALITY,
				NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null),
				ServiceCode.of("1.05"), PlaceOfProvision.PROVIDER, MUNICIPALITY, new BigDecimal("1000.00"),
				new BigDecimal("5.0000"), new BigDecimal(iss), null, List.of(), "Consultoria", "RPS", rps,
				at(3, 1, 8, 0));
		if (!authorized) {
			return document;
		}
		return document.convertToNfse("1", rps, at(3, 1, 9, 0)).send(at(3, 1, 10, 0)).authorize("protocol-" + rps,
				authorizedAt, "xml-ref");
	}
}
