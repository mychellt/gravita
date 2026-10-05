package br.gravita.system.adapter.in.web;

import br.gravita.adapters.inbound.controllers.tax.AccessLogController;
import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.CheckPermissionQuery;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import br.gravita.core.usercases.system.GetAccessLogQuery;
import br.gravita.core.usercases.system.GetAccessLogUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

	@MockitoBean
	private CheckPermissionUseCase checkPermissionUseCase;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	private UserId callerId;

	@BeforeEach
	void setUp() {
		callerId = UserId.generate();
		when(sessionStorePort.resolve("valid-token")).thenReturn(Optional.of(callerId));
	}

	@Test
	@DisplayName("Returns a page of access log entries when the caller is authorized")
	void shouldReturnAPageOfAccessLogEntriesWhenCallerIsAuthorized() throws Exception {
		when(checkPermissionUseCase.execute(any())).thenReturn(true);
		final AccessLog entry = AccessLog.login(UserId.generate(), "jane@example.com", true, "1.2.3.4", "Chrome");
		when(getAccessLogUseCase.execute(any())).thenReturn(new Page<>(List.of(entry), 0, 20, 1));

		mockMvc.perform(get("/api/system/access-log").header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].email").value("jane@example.com"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.page").value(0));

		final ArgumentCaptor<CheckPermissionQuery> captor = ArgumentCaptor.forClass(CheckPermissionQuery.class);
		verify(checkPermissionUseCase).execute(captor.capture());
		assertThat(captor.getValue().userId()).isEqualTo(callerId);
		assertThat(captor.getValue().module()).isEqualTo("system");
		assertThat(captor.getValue().screen()).isEqualTo("access-log");
		assertThat(captor.getValue().action()).isEqualTo(PermissionAction.VIEW);
	}

	@Test
	@DisplayName("Forwards the user id and pagination filters to the use case")
	void shouldForwardUserIdAndPaginationFiltersToTheUseCase() throws Exception {
		when(checkPermissionUseCase.execute(any())).thenReturn(true);
		final UUID userId = UUID.randomUUID();
		when(getAccessLogUseCase.execute(any())).thenReturn(new Page<>(List.of(), 1, 5, 0));

		mockMvc.perform(get("/api/system/access-log")
						.header("Authorization", "Bearer valid-token")
						.param("userId", userId.toString())
						.param("ip", "1.2.3.4")
						.param("device", "Chrome")
						.param("page", "1")
						.param("size", "5"))
				.andExpect(status().isOk());

		final ArgumentCaptor<GetAccessLogQuery> captor = ArgumentCaptor.forClass(GetAccessLogQuery.class);
		verify(getAccessLogUseCase).execute(captor.capture());
		assertThat(captor.getValue().userId()).isEqualTo(UserId.of(userId));
		assertThat(captor.getValue().ip()).isEqualTo("1.2.3.4");
		assertThat(captor.getValue().device()).isEqualTo("Chrome");
		assertThat(captor.getValue().page()).isEqualTo(1);
		assertThat(captor.getValue().size()).isEqualTo(5);
	}

	@Test
	@DisplayName("Responds 403 and skips the query when the caller lacks permission")
	void shouldReturnForbiddenAndSkipTheQueryWhenCallerLacksPermission() throws Exception {
		when(checkPermissionUseCase.execute(any())).thenReturn(false);

		mockMvc.perform(get("/api/system/access-log").header("Authorization", "Bearer valid-token"))
				.andExpect(status().isForbidden());

		verify(getAccessLogUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Responds 401 when no session token is provided")
	void shouldReturnUnauthorizedWhenNoSessionTokenIsProvided() throws Exception {
		mockMvc.perform(get("/api/system/access-log"))
				.andExpect(status().isUnauthorized());

		verify(checkPermissionUseCase, never()).execute(any());
		verify(getAccessLogUseCase, never()).execute(any());
	}
}
