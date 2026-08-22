package br.gravita.adapters.inbound.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerRestController.class)
class CustomerRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldReturn201WhenRegisteringCustomer() throws Exception {
		mockMvc.perform(post("/api/customers")
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isCreated());
	}

	@Test
	void shouldReturn200WhenFindingCustomer() throws Exception {
		mockMvc.perform(get("/api/customers/" + UUID.randomUUID()))
				.andExpect(status().isOk());
	}
}
