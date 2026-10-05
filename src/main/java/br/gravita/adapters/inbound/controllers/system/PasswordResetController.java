package br.gravita.adapters.inbound.controllers.system;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.PasswordResetRejectedException;
import br.gravita.core.usercases.system.ConfirmPasswordResetUseCase;
import br.gravita.core.usercases.system.RequestPasswordResetUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public, unauthenticated endpoints behind "Esqueci minha senha" and the link in the reset e-mail. */
@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

	/** One constant body for every request outcome, so the endpoint never reveals whether an e-mail has an account. */
	static final Map<String, String> REQUEST_ACCEPTED = Map.of("message",
			"Se este e-mail tiver uma conta, enviaremos um link de redefinição em instantes.");

	private final RequestPasswordResetUseCase requestPasswordResetUseCase;
	private final ConfirmPasswordResetUseCase confirmPasswordResetUseCase;

	public PasswordResetController(RequestPasswordResetUseCase requestPasswordResetUseCase,
			ConfirmPasswordResetUseCase confirmPasswordResetUseCase) {
		this.requestPasswordResetUseCase = requestPasswordResetUseCase;
		this.confirmPasswordResetUseCase = confirmPasswordResetUseCase;
	}

	@PostMapping
	public ResponseEntity<Map<String, String>> request(@Valid @RequestBody PasswordResetRequest request) {
		requestPasswordResetUseCase.execute(request.email());
		return ResponseEntity.accepted().body(REQUEST_ACCEPTED);
	}

	/** The token travels in the body, not the URL, so it stays out of access logs and browser history. */
	@PostMapping("/confirm")
	public ResponseEntity<Map<String, String>> confirm(@Valid @RequestBody PasswordResetConfirmRequest request) {
		confirmPasswordResetUseCase.execute(request.token(), request.newPassword());
		return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso. Você já pode entrar."));
	}

	/** An unknown token is a bad request; one that was real but is spent or stale is gone for good. */
	@ExceptionHandler(PasswordResetRejectedException.class)
	public ResponseEntity<Map<String, String>> handleRejected(PasswordResetRejectedException exception) {
		HttpStatus status = exception.getReason() == PasswordResetRejectedException.Reason.INVALID
				? HttpStatus.BAD_REQUEST
				: HttpStatus.GONE;
		return ResponseEntity.status(status)
				.body(Map.of("message", exception.getMessage(), "reason", exception.getReason().name()));
	}

	/**
	 * Answered here so Spring's default handler never logs the rejected value, which can be a password.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleInvalidBody() {
		return ResponseEntity.badRequest().body(Map.of("message", "Dados inválidos."));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
