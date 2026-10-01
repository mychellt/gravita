package br.gravita.tax.adapter.in.web;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class EnterInboundNfeManuallyEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("Creates an inbound NF-e pending conference from fully manual data")
	void enteringFullyManualDataCreatesAPendingConferenceInboundNfe() throws Exception {
		mockMvc.perform(post("/api/nfe/inbound").contentType(MediaType.APPLICATION_JSON)
						.content(manualEntryJson(UUID.randomUUID(), "35240111222333000181550010000012345123456789")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.accessKey").value("35240111222333000181550010000012345123456789"))
				.andExpect(jsonPath("$.supplierName").value("Fornecedor Exemplo LTDA"))
				.andExpect(jsonPath("$.status").value("PENDING_CONFERENCE"));
	}

	@Test
	@DisplayName("Rejects with 400 a request that gives only an access key and no manual data")
	void anAccessKeyAloneWithNoManualDataIsRejectedWith400() throws Exception {
		mockMvc.perform(post("/api/nfe/inbound").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "companyId": "%s",
								  "accessKey": "35240111222333000181550010000012345123456789"
								}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest());
	}

	private String manualEntryJson(UUID companyId, String accessKey) {
		return """
				{
				  "companyId": "%s",
				  "manualData": {
				    "accessKey": "%s",
				    "series": "1",
				    "number": "12345",
				    "supplierCnpj": "11222333000181",
				    "supplierName": "Fornecedor Exemplo LTDA",
				    "issuedAt": "2026-01-15T10:00:00Z",
				    "items": [
				      {
				        "supplierProductCode": "SKU-001",
				        "description": "Parafuso Sextavado M8",
				        "ncm": "73181500",
				        "cfop": "5102",
				        "unit": "UN",
				        "quantity": 100.0000,
				        "unitValue": 1.5000,
				        "totalValue": 150.00,
				        "icmsValue": 27.00,
				        "ipiValue": 0,
				        "pisValue": 2.48,
				        "cofinsValue": 11.40
				      }
				    ],
				    "totals": {
				      "productsValue": 150.00,
				      "freightValue": 15.00,
				      "insuranceValue": 0,
				      "discountValue": 0,
				      "otherExpensesValue": 0,
				      "icmsValue": 27.00,
				      "ipiValue": 0,
				      "pisValue": 2.48,
				      "cofinsValue": 11.40,
				      "totalValue": 165.00
				    }
				  }
				}
				""".formatted(companyId, accessKey);
	}
}
