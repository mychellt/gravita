package br.gravita.masterdata.adapter.in.web;

import br.gravita.adapters.inbound.controllers.masterdata.controllers.CompanyController;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.ConfigureDocumentSeriesRequest;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesCommand;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesUseCase;
import br.gravita.core.ports.inbound.masterdata.RegisterCompanyUseCase;
import br.gravita.core.ports.inbound.masterdata.SwitchSefazEnvironmentUseCase;
import br.gravita.core.ports.inbound.masterdata.UploadDigitalCertificateCommand;
import br.gravita.core.ports.inbound.masterdata.UploadDigitalCertificateUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RegisterCompanyUseCase registerCompanyUseCase;

	@MockitoBean
	private SwitchSefazEnvironmentUseCase switchSefazEnvironmentUseCase;

	@MockitoBean
	private ConfigureDocumentSeriesUseCase configureDocumentSeriesUseCase;

	@MockitoBean
	private UploadDigitalCertificateUseCase uploadDigitalCertificateUseCase;

	@Test
	@DisplayName("Responds 204 when configuring a document series succeeds")
	void shouldReturn204WhenConfiguringDocumentSeriesSucceeds() throws Exception {
		UUID companyId = UUID.randomUUID();
		ConfigureDocumentSeriesRequest request = new ConfigureDocumentSeriesRequest("001", 1000L);

		mockMvc.perform(put("/api/companies/" + companyId + "/document-series/nfe")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isNoContent());

		ArgumentCaptor<ConfigureDocumentSeriesCommand> captor = ArgumentCaptor.forClass(ConfigureDocumentSeriesCommand.class);
		verify(configureDocumentSeriesUseCase).execute(captor.capture());
		assertThat(captor.getValue().companyId()).isEqualTo(CompanyId.of(companyId));
		assertThat(captor.getValue().documentType()).isEqualTo(FiscalDocumentType.NFE);
		assertThat(captor.getValue().series()).isEqualTo("001");
		assertThat(captor.getValue().nextNumber()).isEqualTo(1000L);
	}

	@Test
	@DisplayName("Responds 400 when the document type is unknown")
	void shouldReturn400WhenDocumentTypeIsUnknown() throws Exception {
		ConfigureDocumentSeriesRequest request = new ConfigureDocumentSeriesRequest("001", 1L);

		mockMvc.perform(put("/api/companies/" + UUID.randomUUID() + "/document-series/invalid")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("invalid")));
	}

	@Test
	@DisplayName("Responds 400 when the series is missing")
	void shouldReturn400WhenSeriesIsMissing() throws Exception {
		String body = objectMapper.writeValueAsString(new ConfigureDocumentSeriesRequest(null, 1L));

		mockMvc.perform(put("/api/companies/" + UUID.randomUUID() + "/document-series/nfce")
						.contentType("application/json")
						.content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Responds 404 when the company or series does not exist")
	void shouldReturn404WhenCompanyOrSeriesDoesNotExist() throws Exception {
		UUID companyId = UUID.randomUUID();
		doThrow(new DocumentSeriesNotFoundException(companyId, FiscalDocumentType.NFSE))
				.when(configureDocumentSeriesUseCase).execute(ArgumentMatchers.any());

		mockMvc.perform(put("/api/companies/" + companyId + "/document-series/nfse")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new ConfigureDocumentSeriesRequest("001", 1L))))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Responds 204 when uploading a digital certificate succeeds")
	void shouldReturn204WhenUploadingCertificateSucceeds() throws Exception {
		UUID companyId = UUID.randomUUID();
		MockMultipartFile pfxFile = new MockMultipartFile("pfxFile", "cert.pfx", "application/x-pkcs12",
				new byte[] {1, 2, 3});

		mockMvc.perform(multipart("/api/companies/" + companyId + "/certificate")
						.file(pfxFile)
						.param("password", "secret"))
				.andExpect(status().isNoContent());

		ArgumentCaptor<UploadDigitalCertificateCommand> captor = ArgumentCaptor.forClass(UploadDigitalCertificateCommand.class);
		verify(uploadDigitalCertificateUseCase).execute(captor.capture());
		assertThat(captor.getValue().companyId()).isEqualTo(CompanyId.of(companyId));
		assertThat(captor.getValue().certificateType()).isEqualTo("A1");
		assertThat(captor.getValue().password()).isEqualTo("secret");
		assertThat(captor.getValue().pfxFile()).isEqualTo(new byte[] {1, 2, 3});
	}

	@Test
	@DisplayName("Responds 400 when the use case rejects the certificate type")
	void shouldReturn400WhenCertificateTypeIsRejectedByTheUseCase() throws Exception {
		UUID companyId = UUID.randomUUID();
		MockMultipartFile pfxFile = new MockMultipartFile("pfxFile", "cert.pfx", "application/x-pkcs12",
				new byte[] {1, 2, 3});
		doThrow(new br.gravita.core.domain.shared.BusinessRuleException("Only A1 certificates are accepted"))
				.when(uploadDigitalCertificateUseCase).execute(any());

		mockMvc.perform(multipart("/api/companies/" + companyId + "/certificate")
						.file(pfxFile)
						.param("password", "secret")
						.param("type", "A3"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("A1")));
	}
}
