package br.gravita.masterdata.adapter.in.web;

import br.gravita.adapters.inbound.controllers.masterdata.controllers.CompanyController;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.ConfigureDocumentSeriesRequest;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.CompanyNotFoundException;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesCommand;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesUseCase;
import br.gravita.core.ports.inbound.masterdata.GetCompanyUseCase;
import br.gravita.core.ports.inbound.masterdata.RegisterCompanyCommand;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

	@MockitoBean
	private GetCompanyUseCase getCompanyUseCase;

	@Test
	@DisplayName("GET answers 200 with the company's profile data")
	void shouldReturnTheCompany() throws Exception {
		CompanyId id = CompanyId.of(UUID.randomUUID());
		when(getCompanyUseCase.execute(id)).thenReturn(company(id));

		mockMvc.perform(get("/api/companies/" + id.value()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.value().toString()))
				.andExpect(jsonPath("$.name").value("Acme Ltda"))
				.andExpect(jsonPath("$.cnpj").value("11222333000181"))
				.andExpect(jsonPath("$.ie").value("123456789"))
				.andExpect(jsonPath("$.im").value("987654"))
				.andExpect(jsonPath("$.cnae").value("6201-5/01"))
				.andExpect(jsonPath("$.taxRegime").value("SIMPLES_NACIONAL"))
				.andExpect(jsonPath("$.simplesOptante").value(true))
				.andExpect(jsonPath("$.address").value("Rua Teste, 100"))
				.andExpect(jsonPath("$.state").value("SP"))
				.andExpect(jsonPath("$.issuingEmail").value("nfe@acme.com"))
				.andExpect(jsonPath("$.phone").value("11999999999"))
				.andExpect(jsonPath("$.logoUrl").value("https://acme.com/logo.png"));
	}

	@Test
	@DisplayName("GET answers 404 with a message for an unknown company")
	void shouldReturn404ForAnUnknownCompany() throws Exception {
		UUID unknown = UUID.randomUUID();
		when(getCompanyUseCase.execute(CompanyId.of(unknown))).thenThrow(new CompanyNotFoundException(unknown));

		mockMvc.perform(get("/api/companies/" + unknown))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(containsString(unknown.toString())));
	}

	@Test
	@DisplayName("PATCH answers 400 with a message when the CNPJ differs from the stored one")
	void shouldReturn400WhenPatchChangesTheCnpj() throws Exception {
		UUID id = UUID.randomUUID();
		when(registerCompanyUseCase.execute(any()))
				.thenThrow(new BusinessRuleException("CNPJ cannot be changed: it identifies the legal entity"));

		mockMvc.perform(patch("/api/companies/" + id)
						.contentType("application/json")
						.content(companyBody("\"cnpj\":\"11444777000161\",")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("CNPJ cannot be changed")));
	}

	@Test
	@DisplayName("PATCH without a CNPJ updates the company and passes no CNPJ to the use case")
	void shouldUpdateWithoutCnpj() throws Exception {
		CompanyId id = CompanyId.of(UUID.randomUUID());
		when(registerCompanyUseCase.execute(any())).thenReturn(id);
		when(getCompanyUseCase.execute(id)).thenReturn(company(id));

		mockMvc.perform(patch("/api/companies/" + id.value())
						.contentType("application/json")
						.content(companyBody("")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Acme Ltda"));

		ArgumentCaptor<RegisterCompanyCommand> captor = ArgumentCaptor.forClass(RegisterCompanyCommand.class);
		verify(registerCompanyUseCase).execute(captor.capture());
		assertThat(captor.getValue().id()).isEqualTo(id);
		assertThat(captor.getValue().cnpj()).isNull();
		assertThat(captor.getValue().name()).isEqualTo("Acme Ltda");
	}

	@Test
	@DisplayName("PATCH with the stored CNPJ passes it to the use case")
	void shouldUpdateWithTheSameCnpj() throws Exception {
		CompanyId id = CompanyId.of(UUID.randomUUID());
		when(registerCompanyUseCase.execute(any())).thenReturn(id);
		when(getCompanyUseCase.execute(id)).thenReturn(company(id));

		mockMvc.perform(patch("/api/companies/" + id.value())
						.contentType("application/json")
						.content(companyBody("\"cnpj\":\"11.222.333/0001-81\",")))
				.andExpect(status().isOk());

		ArgumentCaptor<RegisterCompanyCommand> captor = ArgumentCaptor.forClass(RegisterCompanyCommand.class);
		verify(registerCompanyUseCase).execute(captor.capture());
		assertThat(captor.getValue().cnpj()).isEqualTo(Document.cnpj("11222333000181"));
	}

	@Test
	@DisplayName("Responds 400 when the name is blank on register and on update")
	void shouldRejectBlankName() throws Exception {
		String body = companyBody("\"cnpj\":\"11222333000181\",").replace("\"Acme Ltda\"", "\" \"");

		mockMvc.perform(post("/api/companies").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
		mockMvc.perform(patch("/api/companies/" + UUID.randomUUID()).contentType("application/json").content(body))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(registerCompanyUseCase);
	}

	private String companyBody(String cnpjField) {
		return "{" + cnpjField + "\"name\":\"Acme Ltda\",\"ie\":\"123456789\",\"im\":\"987654\","
				+ "\"cnae\":\"6201-5/01\",\"taxRegime\":\"SIMPLES_NACIONAL\",\"simplesOptante\":true,"
				+ "\"address\":\"Rua Teste, 100\",\"state\":\"SP\",\"issuingEmail\":\"nfe@acme.com\","
				+ "\"phone\":\"11999999999\"}";
	}

	private Company company(CompanyId id) {
		return Company.of(id, "Acme Ltda", Document.cnpj("11222333000181"), "123456789", "987654", "6201-5/01",
				TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfe@acme.com", "11999999999", "https://acme.com/logo.png", null);
	}

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
