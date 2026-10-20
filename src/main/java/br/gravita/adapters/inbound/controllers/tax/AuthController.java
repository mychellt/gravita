package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.usercases.system.AuthResult;
import br.gravita.core.usercases.system.AuthStatus;
import br.gravita.core.usercases.system.AuthenticateUseCase;
import br.gravita.core.usercases.system.GetCurrentUserUseCase;
import br.gravita.core.usercases.system.LogoutUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final String BEARER = "Bearer ";

	private final AuthenticateUseCase authenticateUseCase;
	private final GetCurrentUserUseCase getCurrentUserUseCase;
	private final LogoutUseCase logoutUseCase;

	public AuthController(final AuthenticateUseCase authenticateUseCase, final GetCurrentUserUseCase getCurrentUserUseCase,
			final LogoutUseCase logoutUseCase) {
		this.authenticateUseCase = authenticateUseCase;
		this.getCurrentUserUseCase = getCurrentUserUseCase;
		this.logoutUseCase = logoutUseCase;
	}

	/** The logged user behind {@code Authorization: Bearer <sessionToken>}; 401 when there is no valid session. */
	@GetMapping("/me")
	public ResponseEntity<CurrentUserResponse> me(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) final String authorization) {
		return getCurrentUserUseCase.execute(bearerToken(authorization))
				.map(user -> ResponseEntity.ok(CurrentUserResponse.from(user)))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
	}

	/** Ends the session behind the bearer token. Always 204: logging out twice, or with a stale token, is not an error. */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) final String authorization) {
		logoutUseCase.execute(bearerToken(authorization));
		return ResponseEntity.noContent().build();
	}

	private static String bearerToken(final String authorization) {
		return authorization != null && authorization.startsWith(BEARER) ? authorization.substring(BEARER.length()).strip() : null;
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody final LoginRequest request, final HttpServletRequest servletRequest) {
		final AuthResult result = authenticateUseCase.execute(
				request.toCommand(servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
		return toResponse(result);
	}

	@PostMapping("/2fa/verify")
	public ResponseEntity<AuthResponse> verifyTwoFactor(@Valid @RequestBody final TwoFactorVerifyRequest request,
			final HttpServletRequest servletRequest) {
		final AuthResult result = authenticateUseCase.execute(
				request.toCommand(servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
		return toResponse(result);
	}

	private ResponseEntity<AuthResponse> toResponse(final AuthResult result) {
		final HttpStatus status = result.status() == AuthStatus.REJECTED ? HttpStatus.UNAUTHORIZED : HttpStatus.OK;
		return ResponseEntity.status(status).body(AuthResponse.from(result));
	}
}
