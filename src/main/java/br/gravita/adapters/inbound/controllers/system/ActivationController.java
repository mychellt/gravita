package br.gravita.adapters.inbound.controllers.system;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.ActivationRejectedException;
import br.gravita.core.usercases.system.ActivateAccountUseCase;
import br.gravita.core.usercases.system.ResendActivationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public, unauthenticated target of the link in the activation e-mail. */
@RestController
@RequestMapping("/api/activate")
public class ActivationController {

	/** One constant body for every resend outcome, so the endpoint never reveals whether an e-mail has an account. */
	static final Map<String, String> RESEND_ACCEPTED = Map.of("message",
			"Se este e-mail estiver aguardando ativação, enviaremos um novo link em instantes.");

	private final ActivateAccountUseCase activateAccountUseCase;
	private final ResendActivationUseCase resendActivationUseCase;

	public ActivationController(ActivateAccountUseCase activateAccountUseCase,
			ResendActivationUseCase resendActivationUseCase) {
		this.activateAccountUseCase = activateAccountUseCase;
		this.resendActivationUseCase = resendActivationUseCase;
	}

	/** The token is optional at the binding level so a missing one gets the same JSON rejection as a bad one. */
	@GetMapping
	public ResponseEntity<Map<String, String>> activate(@RequestParam(required = false) String token) {
		activateAccountUseCase.execute(token);
		return ResponseEntity.ok(Map.of("message", "Conta ativada com sucesso. Você já pode entrar."));
	}

	@PostMapping("/resend")
	public ResponseEntity<Map<String, String>> resend(@Valid @RequestBody ResendActivationRequest request) {
		resendActivationUseCase.execute(request.email());
		return ResponseEntity.accepted().body(RESEND_ACCEPTED);
	}

	/** An unknown token is a bad request; one that was real but is spent or stale is gone for good. */
	@ExceptionHandler(ActivationRejectedException.class)
	public ResponseEntity<Map<String, String>> handleActivationRejected(ActivationRejectedException exception) {
		HttpStatus status = exception.getReason() == ActivationRejectedException.Reason.INVALID
				? HttpStatus.BAD_REQUEST
				: HttpStatus.GONE;
		return ResponseEntity.status(status)
				.body(Map.of("message", exception.getMessage(), "reason", exception.getReason().name()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
