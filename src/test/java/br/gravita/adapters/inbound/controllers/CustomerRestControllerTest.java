package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.RegisterCustomerRequest;
import br.gravita.adapters.dtos.request.RegisterCustomerRequest.AddressRequest;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.PersonType;
import br.gravita.core.ports.business.CustomerRegistrationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerRestController.class)
class CustomerRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CustomerRegistrationPort customerRegistrationPort;

	private final RegisterCustomerRequest request = new RegisterCustomerRequest(
			PersonType.INDIVIDUAL,
			"111.444.777-35",
			"Maria Silva",
			"maria@example.com",
			null,
			null,
			new BigDecimal("1000.00"),
			List.of(new AddressRequest(AddressType.BILLING, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01000-000", true)),
			List.of(),
			List.of());

	@Test
	void shouldReturn201WhenRegisteringCustomer() throws Exception {
		CustomerDomain created = request.toDomain();
		created.setId(UUID.randomUUID());
		created.setStatus(CustomerStatus.REGULAR);
		created.setCurrentBalance(BigDecimal.ZERO);
		when(customerRegistrationPort.execute(any())).thenReturn(created);

		mockMvc.perform(post("/api/customers")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Maria Silva"))
				.andExpect(jsonPath("$.status").value("REGULAR"))
				.andExpect(jsonPath("$.currentBalance").value(0));
	}

	@Test
	void shouldReturn400WhenNoAddressProvided() throws Exception {
		RegisterCustomerRequest invalid = new RegisterCustomerRequest(
				PersonType.INDIVIDUAL, "111.444.777-35", "Maria Silva", null, null, null, null, List.of(), List.of(), List.of());

		mockMvc.perform(post("/api/customers")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(invalid)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
		mockMvc.perform(post("/api/customers")
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturn200WhenFindingCustomer() throws Exception {
		mockMvc.perform(get("/api/customers/" + UUID.randomUUID()))
				.andExpect(status().isOk());
	}
}
