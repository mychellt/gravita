package br.gravita.adapters.inbound.controllers.security;

import br.gravita.core.domain.exceptions.UnauthorizedException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserArgumentResolverTest {

	@Mock
	private SessionStorePort sessionStorePort;

	@Mock
	private ObjectProvider<SessionStorePort> sessionStorePortProvider;

	@Mock
	private NativeWebRequest webRequest;

	private AuthenticatedUserArgumentResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new AuthenticatedUserArgumentResolver(sessionStorePortProvider);
	}

	private MethodParameter annotatedParameter() throws NoSuchMethodException {
		Method method = Dummy.class.getDeclaredMethod("handle", UserId.class);
		return new MethodParameter(method, 0);
	}

	private static class Dummy {
		void handle(@AuthenticatedUser UserId userId) {
		}
	}

	@Test
	void shouldSupportOnlyAuthenticatedUserAnnotatedUserIdParameters() throws NoSuchMethodException {
		assertThat(resolver.supportsParameter(annotatedParameter())).isTrue();
	}

	@Test
	void shouldResolveTheUserIdBoundToTheBearerToken() {
		UserId userId = UserId.generate();
		when(sessionStorePortProvider.getObject()).thenReturn(sessionStorePort);
		when(webRequest.getHeader("Authorization")).thenReturn("Bearer abc-123");
		when(sessionStorePort.resolve("abc-123")).thenReturn(Optional.of(userId));

		Object resolved = resolver.resolveArgument(null, null, webRequest, null);

		assertThat(resolved).isEqualTo(userId);
	}

	@Test
	void shouldRejectAMissingAuthorizationHeader() {
		when(webRequest.getHeader("Authorization")).thenReturn(null);

		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void shouldRejectAHeaderWithoutTheBearerPrefix() {
		when(webRequest.getHeader("Authorization")).thenReturn("abc-123");

		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void shouldRejectATokenThatDoesNotResolveToAUser() {
		when(sessionStorePortProvider.getObject()).thenReturn(sessionStorePort);
		when(webRequest.getHeader("Authorization")).thenReturn("Bearer unknown-token");
		when(sessionStorePort.resolve("unknown-token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
				.isInstanceOf(UnauthorizedException.class);
	}
}
