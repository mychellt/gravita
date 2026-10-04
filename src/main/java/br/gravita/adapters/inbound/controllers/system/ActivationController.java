package br.gravita.adapters.inbound.controllers.system;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.ActivationRejectedException;
import br.gravita.core.usercases.system.ActivateAccountUseCase;
import br.gravita.core.usercases.system.ResendActivationUseCase;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Public, unauthenticated target of the link in the activation e-mail. The link validates the token and then sends
 * the browser to the activation page with the outcome; the token itself never travels on to the page.
 */
@Slf4j
@RestController
@RequestMapping("/api/activate")
public class ActivationController {

	/** One constant body for every resend outcome, so the endpoint never reveals whether an e-mail has an account. */
	static final Map<String, String> RESEND_ACCEPTED = Map.of("message",
			"Se este e-mail estiver aguardando ativação, enviaremos um novo link em instantes.");

	private final ActivateAccountUseCase activateAccountUseCase;
	private final ResendActivationUseCase resendActivationUseCase;
	private final String resultPageUrl;

	public ActivationController(ActivateAccountUseCase activateAccountUseCase,
			ResendActivationUseCase resendActivationUseCase,
			@Value("${gravita.activation.result-page-url}") String resultPageUrl) {
		this.activateAccountUseCase = activateAccountUseCase;
		this.resendActivationUseCase = resendActivationUseCase;
		this.resultPageUrl = resultPageUrl;
	}

	/**
	 * Validates the token and redirects to the activation page with {@code ?status=} one of {@code activated},
	 * {@code expired}, {@code used}, {@code invalid} or {@code unavailable}. The token is optional at the binding
	 * level so a missing one lands on the same page as a bad one.
	 */
	@GetMapping
	public ResponseEntity<Void> activate(@RequestParam(required = false) String token) {
		return redirectTo(outcomeOf(token));
	}

	private String outcomeOf(String token) {
		try {
			activateAccountUseCase.execute(token);
			return "activated";
		} catch (ActivationRejectedException e) {
			return switch (e.getReason()) {
				case EXPIRED -> "expired";
				case ALREADY_USED -> "used";
				case INVALID -> "invalid";
			};
		} catch (BusinessRuleException e) {
			// e.g. a deactivated account: an old link must never reopen it, and the page treats it as unusable.
			return "invalid";
		} catch (RuntimeException e) {
			log.error("Account activation failed unexpectedly", e);
			return "unavailable";
		}
	}

	private ResponseEntity<Void> redirectTo(String status) {
		var location = UriComponentsBuilder.fromUriString(resultPageUrl).queryParam("status", status).build().toUri();
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(location)
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.build();
	}

	@PostMapping("/resend")
	public ResponseEntity<Map<String, String>> resend(@Valid @RequestBody ResendActivationRequest request) {
		resendActivationUseCase.execute(request.email());
		return ResponseEntity.accepted().body(RESEND_ACCEPTED);
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
