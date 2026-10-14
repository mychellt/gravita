package br.gravita.adapters.inbound.controllers.auxiliary;

import br.gravita.adapters.configuration.web.WebMvcConfiguration;
import br.gravita.adapters.inbound.controllers.ApiExceptionHandler;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.TransportModalityDomain;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.CreateTransportModalityPort;
import br.gravita.core.ports.business.DeleteTransportModalityPort;
import br.gravita.core.ports.business.FindTransportModalityPort;
import br.gravita.core.ports.business.ListTransportModalitiesPort;
import br.gravita.core.ports.business.UpdateTransportModalityPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransportModalityRestController.class)
@Import({WebMvcConfiguration.class, ApiExceptionHandler.class})
class TransportModalityRestControllerTest {

	private static final String URL = "/api/auxiliary/transport-modalities";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateTransportModalityPort createPort;
	@MockitoBean
	private FindTransportModalityPort findPort;
	@MockitoBean
	private ListTransportModalitiesPort listPort;
	@MockitoBean
	private UpdateTransportModalityPort updatePort;
	@MockitoBean
	private DeleteTransportModalityPort deletePort;

	private static TransportModalityDomain modality(final UUID id, final String name, final String code, final boolean active) {
		return TransportModalityDomain.builder().id(id).name(name).code(code).active(active).build();
	}

	@Test
	@DisplayName("GET lists the active modalities by default, with id, name, code and active")
	void shouldListActiveModalities() throws Exception {
		final UUID id = UUID.randomUUID();
		when(listPort.execute(any())).thenReturn(List.of(
				modality(id, "CIF — Por conta do emitente", "CIF", true),
				modality(UUID.randomUUID(), "Sem transporte", "NONE", true)));

		mockMvc.perform(get(URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(id.toString()))
				.andExpect(jsonPath("$[0].name").value("CIF — Por conta do emitente"))
				.andExpect(jsonPath("$[0].code").value("CIF"))
				.andExpect(jsonPath("$[0].active").value(true))
				.andExpect(jsonPath("$[1].code").value("NONE"));

		final ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
		verify(listPort).execute(context.capture());
		assertThat(context.getValue().getData(Boolean.class)).isFalse();
	}

	@Test
	@DisplayName("GET ?includeInactive=true asks for inactive modalities too (admin screen)")
	void shouldListInactiveToo() throws Exception {
		when(listPort.execute(any())).thenReturn(List.of(modality(UUID.randomUUID(), "FOB", "FOB", false)));

		mockMvc.perform(get(URL + "?includeInactive=true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].active").value(false));

		final ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
		verify(listPort).execute(context.capture());
		assertThat(context.getValue().getData(Boolean.class)).isTrue();
	}

	@Test
	@DisplayName("POST creates a modality and answers 201 with its location")
	void shouldCreate() throws Exception {
		final UUID id = UUID.randomUUID();
		when(createPort.execute(any())).thenReturn(modality(id, "FOB — Por conta do destinatário", "FOB", true));

		mockMvc.perform(post(URL).contentType("application/json")
						.content("{\"name\":\"FOB — Por conta do destinatário\",\"code\":\"FOB\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", URL + "/" + id))
				.andExpect(jsonPath("$.code").value("FOB"));
	}

	@Test
	@DisplayName("POST without name or code is a 400")
	void shouldValidateRequest() throws Exception {
		mockMvc.perform(post(URL).contentType("application/json").content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("POST of a second active modality with the same code is a 409")
	void shouldReportDuplicate() throws Exception {
		when(createPort.execute(any())).thenThrow(new DuplicateResourceException("already exists"));

		mockMvc.perform(post(URL).contentType("application/json")
						.content("{\"name\":\"CIF\",\"code\":\"CIF\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("PATCH edits the modality and passes the id from the path with the optional active flag")
	void shouldUpdate() throws Exception {
		final UUID id = UUID.randomUUID();
		when(updatePort.execute(any())).thenReturn(modality(id, "CIF renomeada", "CIF", false));

		mockMvc.perform(patch(URL + "/" + id).contentType("application/json")
						.content("{\"name\":\"CIF renomeada\",\"code\":\"CIF\",\"active\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("CIF renomeada"))
				.andExpect(jsonPath("$.active").value(false));

		final ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
		verify(updatePort).execute(context.capture());
		final TransportModalityDomain sent = context.getValue().getData(TransportModalityDomain.class);
		assertThat(sent.getId()).isEqualTo(id);
		assertThat(sent.getActive()).isFalse();
	}

	@Test
	@DisplayName("DELETE answers 204 and goes through the (soft) delete use case")
	void shouldDelete() throws Exception {
		final UUID id = UUID.randomUUID();

		mockMvc.perform(delete(URL + "/" + id)).andExpect(status().isNoContent());

		final ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
		verify(deletePort).execute(context.capture());
		assertThat(context.getValue().getData(UUID.class)).isEqualTo(id);
	}

	@Test
	@DisplayName("GET/DELETE of an unknown id is a 404")
	void shouldReportNotFound() throws Exception {
		final UUID id = UUID.randomUUID();
		when(findPort.execute(any())).thenThrow(new ResourceNotFoundException("missing"));
		when(deletePort.execute(any())).thenThrow(new ResourceNotFoundException("missing"));

		mockMvc.perform(get(URL + "/" + id)).andExpect(status().isNotFound());
		mockMvc.perform(delete(URL + "/" + id)).andExpect(status().isNotFound());
	}
}
