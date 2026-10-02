package br.gravita.adapters.inbound.controllers.system;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.SignupRejectedException;
import br.gravita.core.usercases.system.SignupResult;
import br.gravita.core.usercases.system.SignupUseCase;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

/** Public, unauthenticated self-service signup used by the marketing site's register page. */
@RestController
@RequestMapping("/api/signup")
public class SignupController {

	private final SignupUseCase signupUseCase;

	public SignupController(SignupUseCase signupUseCase) {
		this.signupUseCase = signupUseCase;
	}

	@PostMapping
	public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
		SignupResult result = signupUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/users/" + result.userId()))
				.body(SignupResponse.from(result, request.email().strip()));
	}

	@ExceptionHandler(SignupRejectedException.class)
	public ResponseEntity<Map<String, String>> handleSignupRejected(SignupRejectedException exception) {
		return ResponseEntity.badRequest()
				.body(Map.of("message", exception.getMessage(), "field", exception.getField()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	/** Two signups racing past the duplicate checks still hit the unique constraints; nothing is persisted. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> handleConstraintViolation(DataIntegrityViolationException exception) {
		String cause = String.valueOf(exception.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
		if (!cause.contains("duplicate key")) {
			return ResponseEntity.badRequest().body(Map.of("message", "Dados inválidos. Revise o formulário."));
		}
		boolean duplicateCnpj = cause.contains("document");
		return ResponseEntity.badRequest().body(Map.of(
				"message", duplicateCnpj ? "Este CNPJ já está cadastrado." : "Este e-mail já está cadastrado.",
				"field", duplicateCnpj ? SignupRejectedException.CNPJ : SignupRejectedException.EMAIL));
	}
}
