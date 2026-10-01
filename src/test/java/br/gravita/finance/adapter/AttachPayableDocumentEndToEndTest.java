package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort.DocumentStorageUnavailableException;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AttachPayableDocumentEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@MockitoBean
	private DocumentAttachmentStoragePort documentAttachmentStoragePort;

	private Payable persistOpen() {
		return payableRepositoryPort.save(Payable.createManual(PayableId.of(UUID.randomUUID()), null,
				new BigDecimal("100.00"), LocalDate.now().plusDays(7), null));
	}

	private ResultActions upload(UUID payableId, MockMultipartFile file) throws Exception {
		return mockMvc.perform(multipart("/api/finance/payables/{id}/attachments", payableId).file(file));
	}

	private static MockMultipartFile pdf(String name, byte... content) {
		return new MockMultipartFile("file", name, "application/pdf", content);
	}

	@Test
	@DisplayName("Stores the uploaded file and persists its reference on the payable")
	void storesTheFileAndPersistsItsReferenceOnThePayable() throws Exception {
		Payable payable = persistOpen();
		when(documentAttachmentStoragePort.store(any())).thenReturn("https://files.example.com/boleto.pdf");

		upload(payable.getId().value(), pdf("boleto.pdf", (byte) 1)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.attachments[0]").value("https://files.example.com/boleto.pdf"));

		assertThat(payableRepositoryPort.findById(payable.getId()).orElseThrow().getAttachments())
				.containsExactly("https://files.example.com/boleto.pdf");
	}

	@Test
	@DisplayName("Allows a payable to carry multiple attachments")
	void aPayableCanCarryMultipleAttachments() throws Exception {
		Payable payable = persistOpen();
		when(documentAttachmentStoragePort.store(any())).thenReturn("https://files.example.com/boleto.pdf")
				.thenReturn("https://files.example.com/nf.pdf");

		upload(payable.getId().value(), pdf("boleto.pdf", (byte) 1)).andExpect(status().isCreated());
		upload(payable.getId().value(), pdf("nf.pdf", (byte) 2)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.attachments.length()").value(2));

		assertThat(payableRepositoryPort.findById(payable.getId()).orElseThrow().getAttachments())
				.containsExactly("https://files.example.com/boleto.pdf", "https://files.example.com/nf.pdf");
	}

	@Test
	@DisplayName("Responds 404 Not Found when the payable does not exist")
	void anUnknownPayableIsRejectedWith404() throws Exception {
		upload(UUID.randomUUID(), pdf("boleto.pdf", (byte) 1)).andExpect(status().isNotFound());

		verify(documentAttachmentStoragePort, never()).store(any());
	}

	@Test
	@DisplayName("Responds 400 Bad Request when the uploaded file is empty")
	void anEmptyFileIsRejectedWith400() throws Exception {
		Payable payable = persistOpen();

		upload(payable.getId().value(), pdf("empty.pdf")).andExpect(status().isBadRequest());

		verify(documentAttachmentStoragePort, never()).store(any());
	}

	@Test
	@DisplayName("Responds 502 Bad Gateway on a storage failure and attaches nothing")
	void aStorageFailureIsReportedAs502AndAttachesNothing() throws Exception {
		Payable payable = persistOpen();
		when(documentAttachmentStoragePort.store(any()))
				.thenThrow(new DocumentStorageUnavailableException("not configured"));

		upload(payable.getId().value(), pdf("boleto.pdf", (byte) 1)).andExpect(status().isBadGateway());

		assertThat(payableRepositoryPort.findById(payable.getId()).orElseThrow().getAttachments()).isEmpty();
	}
}
