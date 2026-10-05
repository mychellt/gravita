package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.usercases.system.AuthResult;
import br.gravita.core.usercases.system.AuthStatus;
import br.gravita.core.usercases.system.AuthenticateUseCase;
import br.gravita.core.usercases.system.GetCurrentUserUseCase;
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

	public AuthController(AuthenticateUseCase authenticateUseCase, GetCurrentUserUseCase getCurrentUserUseCase) {
		this.authenticateUseCase = authenticateUseCase;
		this.getCurrentUserUseCase = getCurrentUserUseCase;
	}

	/** The logged user behind {@code Authorization: Bearer <sessionToken>}; 401 when there is no valid session. */
	@GetMapping("/me")
	public ResponseEntity<CurrentUserResponse> me(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		String token = authorization != null && authorization.startsWith(BEARER)
				? authorization.substring(BEARER.length()).strip()
				: null;
		return getCurrentUserUseCase.execute(token)
				.map(user -> ResponseEntity.ok(CurrentUserResponse.from(user)))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
		AuthResult result = authenticateUseCase.execute(
				request.toCommand(servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
		return toResponse(result);
	}

	@PostMapping("/2fa/verify")
	public ResponseEntity<AuthResponse> verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest request,
			HttpServletRequest servletRequest) {
		AuthResult result = authenticateUseCase.execute(
				request.toCommand(servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
		return toResponse(result);
	}

	private ResponseEntity<AuthResponse> toResponse(AuthResult result) {
		HttpStatus status = result.status() == AuthStatus.REJECTED ? HttpStatus.UNAUTHORIZED : HttpStatus.OK;
		return ResponseEntity.status(status).body(AuthResponse.from(result));
	}
}
