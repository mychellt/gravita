package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.inbound.controllers.tax.controllers.VoidDocumentNumberRangeController;
import br.gravita.adapters.inbound.controllers.tax.dtos.VoidNumberRangeRequest;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.domain.tax.VoidedNumberRangeId;
import br.gravita.core.ports.inbound.tax.VoidDocumentNumberRangeUseCase;
import br.gravita.core.ports.inbound.tax.VoidNumberRangeCommand;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(VoidDocumentNumberRangeController.class)
class VoidDocumentNumberRangeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private VoidDocumentNumberRangeUseCase voidDocumentNumberRangeUseCase;

	@Test
	void shouldReturn201WithTheCreatedRecordOnSuccess() throws Exception {
		UUID companyId = UUID.randomUUID();
		VoidedNumberRange created = VoidedNumberRange.of(VoidedNumberRangeId.of(UUID.randomUUID()),
				CompanyId.of(companyId), "001", 100L, 110L, "duplicate numbering skipped", "void-protocol-1",
				Instant.now());
		when(voidDocumentNumberRangeUseCase.execute(any())).thenReturn(created);

		VoidNumberRangeRequest request = new VoidNumberRangeRequest(companyId, "001", 100L, 110L,
				"duplicate numbering skipped");

		mockMvc.perform(post("/api/nfe/void-range")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.series").value("001"))
				.andExpect(jsonPath("$.protocol").value("void-protocol-1"));

		ArgumentCaptor<VoidNumberRangeCommand> captor = ArgumentCaptor.forClass(VoidNumberRangeCommand.class);
		verify(voidDocumentNumberRangeUseCase).execute(captor.capture());
		assertThat(captor.getValue().companyId()).isEqualTo(companyId);
		assertThat(captor.getValue().startNumber()).isEqualTo(100L);
		assertThat(captor.getValue().endNumber()).isEqualTo(110L);
	}

	@Test
	void shouldReturn409WhenTheUseCaseRejectsAMissingJustification() throws Exception {
		doThrow(new BusinessRuleException("justification is required to void a document number range"))
				.when(voidDocumentNumberRangeUseCase).execute(any());

		VoidNumberRangeRequest request = new VoidNumberRangeRequest(UUID.randomUUID(), "001", 100L, 110L, null);

		mockMvc.perform(post("/api/nfe/void-range")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isConflict());
	}

	@Test
	void shouldReturn400WhenSeriesIsMissing() throws Exception {
		String body = """
				{"companyId":"%s","startNumber":100,"endNumber":110,"justification":"reason"}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/nfe/void-range").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());

		verify(voidDocumentNumberRangeUseCase, org.mockito.Mockito.never()).execute(any());
	}
}
