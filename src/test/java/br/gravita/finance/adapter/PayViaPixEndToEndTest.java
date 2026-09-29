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
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.PixPaymentReceipt;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
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
class PayViaPixEndToEndTest {

	private static final String RECEIPT_URL = "https://files.example.com/pix-receipt-E2E1.pdf";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@MockitoBean
	private BankIntegrationPort bankIntegrationPort;

	@MockitoBean
	private DocumentAttachmentStoragePort documentAttachmentStoragePort;

	private Payable persistApproved() {
		return payableRepositoryPort.save(Payable
				.createManual(PayableId.of(UUID.randomUUID()), null, new BigDecimal("100.00"),
						LocalDate.now().plusDays(7), null)
				.approve(UUID.randomUUID()));
	}

	private ResultActions pixPay(UUID payableId, String body) throws Exception {
		return mockMvc.perform(post("/api/finance/payables/{id}/pix-pay", payableId)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void paysThePayableAndPersistsItAsPaidWithTheReceiptAttached() throws Exception {
		Payable payable = persistApproved();
		when(bankIntegrationPort.payViaPix(any()))
				.thenReturn(new PixPaymentReceipt("E2E1", "application/pdf", new byte[] { 1 }));
		when(documentAttachmentStoragePort.store(any())).thenReturn(RECEIPT_URL);

		pixPay(payable.getId().value(), "{\"pixKey\": \"12345678909\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAID"))
				.andExpect(jsonPath("$.attachments[0]").value(RECEIPT_URL));

		Payable stored = payableRepositoryPort.findById(payable.getId()).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(PayableStatus.PAID);
		assertThat(stored.getAttachments()).containsExactly(RECEIPT_URL);
	}

	@Test
	void aTransferFailureIsReportedAs502AndLeavesThePayableApproved() throws Exception {
		Payable payable = persistApproved();
		when(bankIntegrationPort.payViaPix(any()))
				.thenThrow(new BankIntegrationUnavailableException("not configured"));

		pixPay(payable.getId().value(), "{\"pixKey\": \"12345678909\"}").andExpect(status().isBadGateway());

		Payable stored = payableRepositoryPort.findById(payable.getId()).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(PayableStatus.APPROVED);
		assertThat(stored.getAttachments()).isEmpty();
	}

	@Test
	void anOpenPayableIsRejectedWithoutContactingTheBank() throws Exception {
		Payable open = payableRepositoryPort.save(Payable.createManual(PayableId.of(UUID.randomUUID()), null,
				new BigDecimal("100.00"), LocalDate.now().plusDays(7), null));

		pixPay(open.getId().value(), "{\"pixKey\": \"12345678909\"}").andExpect(status().isBadRequest());

		verify(bankIntegrationPort, never()).payViaPix(any());
	}

	@Test
	void anUnknownPayableIsRejectedWith404() throws Exception {
		pixPay(UUID.randomUUID(), "{\"pixKey\": \"12345678909\"}").andExpect(status().isNotFound());
	}

	@Test
	void aRequestWithoutAPixKeyIsRejected() throws Exception {
		Payable payable = persistApproved();

		pixPay(payable.getId().value(), "{\"pixKey\": \"  \"}").andExpect(status().isBadRequest());
		pixPay(payable.getId().value(), "{}").andExpect(status().isBadRequest());

		verify(bankIntegrationPort, never()).payViaPix(any());
	}
}
