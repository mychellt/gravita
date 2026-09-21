package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.usercases.system.AuthResult;
import br.gravita.core.usercases.system.AuthStatus;
import br.gravita.core.usercases.system.AuthenticateUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthenticateUseCase authenticateUseCase;

	public AuthController(AuthenticateUseCase authenticateUseCase) {
		this.authenticateUseCase = authenticateUseCase;
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
