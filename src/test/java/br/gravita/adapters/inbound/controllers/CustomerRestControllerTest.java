package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.RegisterCustomerRequest;
import br.gravita.adapters.dtos.request.RegisterCustomerRequest.AddressRequest;
import br.gravita.adapters.dtos.request.UpdateCustomerRequest;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.exceptions.CustomerNotFoundException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.CustomerRegistrationPort;
import br.gravita.core.ports.business.FindCustomerPort;
import br.gravita.core.ports.business.ListCustomersPort;
import br.gravita.core.ports.inbound.masterdata.UpdateCustomerUseCase;
import br.gravita.core.domain.shared.PersonType;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

	@MockitoBean
	private UpdateCustomerUseCase updateCustomerUseCase;

	@MockitoBean
	private FindCustomerPort findCustomerPort;

	@MockitoBean
	private ListCustomersPort listCustomersPort;

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
	@DisplayName("Returns 201 Created when a customer is registered with valid data")
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
	@DisplayName("Returns 400 Bad Request when a customer is registered without any address")
	void shouldReturn400WhenNoAddressProvided() throws Exception {
		RegisterCustomerRequest invalid = new RegisterCustomerRequest(
				PersonType.INDIVIDUAL, "111.444.777-35", "Maria Silva", null, null, null, null, List.of(), List.of(), List.of());

		mockMvc.perform(post("/api/customers")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(invalid)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Returns 400 Bad Request when required customer fields are missing")
	void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
		mockMvc.perform(post("/api/customers")
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Returns 200 OK with the full customer record when an existing customer is found")
	void shouldReturn200WhenFindingCustomer() throws Exception {
		UUID id = UUID.randomUUID();
		CustomerDomain customer = request.toDomain();
		customer.setId(id);
		customer.setStatus(CustomerStatus.REGULAR);
		customer.setCurrentBalance(new BigDecimal("250.00"));
		when(findCustomerPort.execute(any())).thenReturn(customer);

		mockMvc.perform(get("/api/customers/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.name").value("Maria Silva"))
				.andExpect(jsonPath("$.status").value("REGULAR"))
				.andExpect(jsonPath("$.creditLimit").value(1000.00))
				.andExpect(jsonPath("$.currentBalance").value(250.00))
				.andExpect(jsonPath("$.addresses[0].street").value("Rua A"))
				.andExpect(jsonPath("$.contacts").isArray())
				.andExpect(jsonPath("$.priceTables").isArray());
	}

	@Test
	@DisplayName("Returns 404 Not Found with a message when the customer does not exist")
	void shouldReturn404WhenFindingUnknownCustomer() throws Exception {
		UUID id = UUID.randomUUID();
		when(findCustomerPort.execute(any())).thenThrow(new CustomerNotFoundException(id));

		mockMvc.perform(get("/api/customers/" + id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Customer not found: " + id));
	}

	@Test
	@DisplayName("Returns 200 OK with the list of customers")
	void shouldReturn200WhenListingCustomers() throws Exception {
		CustomerDomain customer = request.toDomain();
		customer.setId(UUID.randomUUID());
		customer.setStatus(CustomerStatus.REGULAR);
		customer.setCurrentBalance(BigDecimal.ZERO);
		when(listCustomersPort.execute(any())).thenReturn(List.of(customer));

		mockMvc.perform(get("/api/customers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Maria Silva"))
				.andExpect(jsonPath("$[0].document").exists())
				.andExpect(jsonPath("$[0].status").value("REGULAR"))
				.andExpect(jsonPath("$[0].creditLimit").value(1000.00))
				.andExpect(jsonPath("$[0].currentBalance").value(0));
	}

	@Test
	@DisplayName("Returns 200 OK with an empty array when there are no customers")
	void shouldReturnEmptyArrayWhenNoCustomers() throws Exception {
		when(listCustomersPort.execute(any())).thenReturn(List.of());

		mockMvc.perform(get("/api/customers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Returns 204 No Content when a customer is updated successfully")
	void shouldReturn204WhenUpdatingCustomer() throws Exception {
		UUID id = UUID.randomUUID();
		UpdateCustomerRequest update = new UpdateCustomerRequest(
				null, null, "Maria S. Costa", null, null, null, null, null, null, null, null, null);

		mockMvc.perform(patch("/api/customers/" + id)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(update)))
				.andExpect(status().isNoContent());

		verify(updateCustomerUseCase).execute(update.toCommand(id));
	}

	@Test
	@DisplayName("Returns 404 Not Found when updating a customer that does not exist")
	void shouldReturn404WhenUpdatingUnknownCustomer() throws Exception {
		UUID id = UUID.randomUUID();
		UpdateCustomerRequest update = new UpdateCustomerRequest(
				null, null, "Maria S. Costa", null, null, null, null, null, null, null, null, null);
		doThrow(new ResourceNotFoundException("Customer not found: " + id)).when(updateCustomerUseCase).execute(any());

		mockMvc.perform(patch("/api/customers/" + id)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(update)))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Returns 409 Conflict when the document is updated without informing the person type")
	void shouldReturn409WhenUpdatingDocumentWithoutType() throws Exception {
		UUID id = UUID.randomUUID();
		UpdateCustomerRequest update = new UpdateCustomerRequest(
				null, "111.444.777-35", null, null, null, null, null, null, null, null, null, null);

		mockMvc.perform(patch("/api/customers/" + id)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(update)))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Returns 409 Conflict when the document is updated with an invalid check digit")
	void shouldReturn409WhenUpdatingDocumentWithInvalidCheckDigit() throws Exception {
		UUID id = UUID.randomUUID();
		UpdateCustomerRequest update = new UpdateCustomerRequest(
				PersonType.INDIVIDUAL, "111.444.777-36", null, null, null, null, null, null, null, null, null, null);

		mockMvc.perform(patch("/api/customers/" + id)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(update)))
				.andExpect(status().isConflict());
	}
}
