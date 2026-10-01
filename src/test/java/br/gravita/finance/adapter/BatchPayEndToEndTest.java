package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.finance.BankIntegrationUnavailableException;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.IssuedRemittance;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class BatchPayEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@MockitoBean
	private BankIntegrationPort bankIntegrationPort;

	private Payable persistApproved(String amount) {
		return payableRepositoryPort.save(Payable
				.createManual(PayableId.of(UUID.randomUUID()), null, new BigDecimal(amount),
						LocalDate.now().plusDays(7), null)
				.approve(UUID.randomUUID()));
	}

	private Payable persistOpen(String amount) {
		return payableRepositoryPort.save(Payable.createManual(PayableId.of(UUID.randomUUID()), null,
				new BigDecimal(amount), LocalDate.now().plusDays(7), null));
	}

	private ResultActions batchPay(String bank, UUID... payableIds) throws Exception {
		StringBuilder ids = new StringBuilder();
		for (UUID id : payableIds) {
			ids.append(ids.length() == 0 ? "" : ",").append('"').append(id).append('"');
		}
		return mockMvc.perform(post("/api/finance/payables/batch-pay").contentType(MediaType.APPLICATION_JSON)
				.content("{\"payableIds\": [%s], \"bankIntegration\": %s}".formatted(ids, bank)));
	}

	@Test
	@DisplayName("Generates a single remittance referencing every selected payable and leaves them approved")
	void generatesOneRemittanceReferencingEveryPayableAndLeavesThemApproved() throws Exception {
		Payable first = persistApproved("100.00");
		Payable second = persistApproved("40.25");
		when(bankIntegrationPort.sendRemittance(any())).thenReturn(new IssuedRemittance("REM-1", "cnab-payload"));

		batchPay("\"ITAU\"", first.getId().value(), second.getId().value()).andExpect(status().isOk())
				.andExpect(jsonPath("$.reference").value("REM-1"))
				.andExpect(jsonPath("$.bankIntegration").value("ITAU"))
				.andExpect(jsonPath("$.payableIds[0]").value(first.getId().value().toString()))
				.andExpect(jsonPath("$.payableIds[1]").value(second.getId().value().toString()))
				.andExpect(jsonPath("$.totalAmount").value(140.25))
				.andExpect(jsonPath("$.fileContent").value("cnab-payload"));

		assertThat(payableRepositoryPort.findById(first.getId()).orElseThrow().getStatus())
				.isEqualTo(PayableStatus.APPROVED);
		assertThat(payableRepositoryPort.findById(second.getId()).orElseThrow().getStatus())
				.isEqualTo(PayableStatus.APPROVED);
	}

	@Test
	@DisplayName("Rejects a batch containing a still-open payable without contacting the bank")
	void rejectsABatchWithAPayableThatIsStillOpenWithoutContactingTheBank() throws Exception {
		Payable approved = persistApproved("100.00");
		Payable open = persistOpen("40.00");

		batchPay("\"ITAU\"", approved.getId().value(), open.getId().value()).andExpect(status().isBadRequest());

		verify(bankIntegrationPort, never()).sendRemittance(any());
	}

	@Test
	@DisplayName("Responds 404 Not Found when a selected payable does not exist")
	void anUnknownPayableIsRejectedWith404() throws Exception {
		batchPay("\"ITAU\"", UUID.randomUUID()).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Responds 502 Bad Gateway when the bank is unavailable")
	void aBankThatIsNotAvailableIsReportedAs502() throws Exception {
		Payable approved = persistApproved("100.00");
		when(bankIntegrationPort.sendRemittance(any())).thenThrow(
				new BankIntegrationUnavailableException("not configured"));

		batchPay("\"ITAU\"", approved.getId().value()).andExpect(status().isBadGateway());
	}

	@Test
	@DisplayName("Rejects a batch payment with no payables selected")
	void anEmptySelectionIsRejected() throws Exception {
		batchPay("\"ITAU\"").andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Rejects a batch payment request that does not name a bank")
	void aRequestWithoutABankIsRejected() throws Exception {
		Payable approved = persistApproved("100.00");

		batchPay("null", approved.getId().value()).andExpect(status().isBadRequest());
	}
}
