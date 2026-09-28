package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.repositories.finance.DocumentAttachmentJpaRepository;
import br.gravita.core.domain.finance.AttachmentFile;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableAttachment;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.ports.inbound.finance.AttachPayableDocumentCommand;
import br.gravita.core.ports.inbound.finance.AttachPayableDocumentUseCase;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AttachPayableDocumentIntegrationTest {

	@Autowired
	private AttachPayableDocumentUseCase attachPayableDocumentUseCase;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@Autowired
	private DocumentAttachmentJpaRepository documentAttachmentJpaRepository;

	@Autowired
	private MockMvc mockMvc;

	private Payable savedPayable() {
		return payableRepositoryPort.save(Payable.createManual(PayableId.of(UUID.randomUUID()), null,
				new BigDecimal("300.00"), LocalDate.now().plusDays(5),
				List.of(new CostCenterShare(UUID.randomUUID(), new BigDecimal("100")))));
	}

	@Test
	void persistsTheFileAndEveryReferenceInTheOrderAttached() {
		Payable payable = savedPayable();
		byte[] boleto = { 1, 2, 3 };

		attachPayableDocumentUseCase.execute(new AttachPayableDocumentCommand(payable.getId().value(),
				new AttachmentFile("boleto.pdf", "application/pdf", boleto)));
		Payable result = attachPayableDocumentUseCase.execute(new AttachPayableDocumentCommand(
				payable.getId().value(), new AttachmentFile("nf.xml", "text/xml", new byte[] { 9, 9 })));

		Payable found = payableRepositoryPort.findById(payable.getId()).orElseThrow();
		assertThat(found.getAttachments()).extracting(PayableAttachment::fileName).containsExactly("boleto.pdf",
				"nf.xml");
		assertThat(result.getAttachments()).isEqualTo(found.getAttachments());
		PayableAttachment first = found.getAttachments().get(0);
		assertThat(first.contentType()).isEqualTo("application/pdf");
		assertThat(first.sizeBytes()).isEqualTo(3);
		assertThat(documentAttachmentJpaRepository.findById(UUID.fromString(first.storageRef())).orElseThrow()
				.getContent()).isEqualTo(boleto);
		assertThat(found.getCostCenterSplit()).hasSize(1);
	}

	@Test
	void anAttachedPayableSurvivesLaterChangesToItsSplit() {
		Payable payable = savedPayable();
		attachPayableDocumentUseCase.execute(new AttachPayableDocumentCommand(payable.getId().value(),
				new AttachmentFile("boleto.pdf", "application/pdf", new byte[] { 1 })));

		Payable reloaded = payableRepositoryPort.findById(payable.getId()).orElseThrow();
		payableRepositoryPort.save(reloaded.withCostCenterSplit(
				List.of(new CostCenterShare(UUID.randomUUID(), new BigDecimal("100")))));

		assertThat(payableRepositoryPort.findById(payable.getId()).orElseThrow().getAttachments()).hasSize(1);
	}

	@Test
	void postingAMultipartFileAttachesItAndReturns201() throws Exception {
		Payable payable = savedPayable();

		mockMvc.perform(multipart("/api/finance/payables/{id}/attachments", payable.getId().value())
						.file(new MockMultipartFile("file", "recibo.pdf", "application/pdf", new byte[] { 1, 2 })))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(payable.getId().value().toString()))
				.andExpect(jsonPath("$.attachments.length()").value(1))
				.andExpect(jsonPath("$.attachments[0].fileName").value("recibo.pdf"))
				.andExpect(jsonPath("$.attachments[0].sizeBytes").value(2));

		mockMvc.perform(multipart("/api/finance/payables/{id}/attachments", payable.getId().value())
						.file(new MockMultipartFile("file", "nf.pdf", "application/pdf", new byte[] { 3 })))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.attachments.length()").value(2));
	}

	@Test
	void postingToAnUnknownPayableReturns404() throws Exception {
		mockMvc.perform(multipart("/api/finance/payables/{id}/attachments", UUID.randomUUID())
						.file(new MockMultipartFile("file", "recibo.pdf", "application/pdf", new byte[] { 1 })))
				.andExpect(status().isNotFound());
	}

	@Test
	void postingAnEmptyFileReturns400() throws Exception {
		Payable payable = savedPayable();

		mockMvc.perform(multipart("/api/finance/payables/{id}/attachments", payable.getId().value())
						.file(new MockMultipartFile("file", "vazio.pdf", "application/pdf", new byte[0])))
				.andExpect(status().isBadRequest());
	}
}
