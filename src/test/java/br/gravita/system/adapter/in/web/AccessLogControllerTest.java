package br.gravita.system.adapter.in.web;

import br.gravita.adapters.inbound.controllers.tax.AccessLogController;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.usercases.system.GetAccessLogQuery;
import br.gravita.core.usercases.system.GetAccessLogUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccessLogController.class)
class AccessLogControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GetAccessLogUseCase getAccessLogUseCase;

	@Test
	void shouldReturnAPageOfAccessLogEntries() throws Exception {
		AccessLog entry = AccessLog.login(UserId.generate(), "jane@example.com", true, "1.2.3.4", "Chrome");
		when(getAccessLogUseCase.execute(any())).thenReturn(new Page<>(List.of(entry), 0, 20, 1));

		mockMvc.perform(get("/api/system/access-log"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].email").value("jane@example.com"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.page").value(0));
	}

	@Test
	void shouldForwardUserIdAndPaginationFiltersToTheUseCase() throws Exception {
		UUID userId = UUID.randomUUID();
		when(getAccessLogUseCase.execute(any())).thenReturn(new Page<>(List.of(), 1, 5, 0));

		mockMvc.perform(get("/api/system/access-log")
						.param("userId", userId.toString())
						.param("ip", "1.2.3.4")
						.param("device", "Chrome")
						.param("page", "1")
						.param("size", "5"))
				.andExpect(status().isOk());

		ArgumentCaptor<GetAccessLogQuery> captor = ArgumentCaptor.forClass(GetAccessLogQuery.class);
		verify(getAccessLogUseCase).execute(captor.capture());
		assertThat(captor.getValue().userId()).isEqualTo(UserId.of(userId));
		assertThat(captor.getValue().ip()).isEqualTo("1.2.3.4");
		assertThat(captor.getValue().device()).isEqualTo("Chrome");
		assertThat(captor.getValue().page()).isEqualTo(1);
		assertThat(captor.getValue().size()).isEqualTo(5);
	}
}
