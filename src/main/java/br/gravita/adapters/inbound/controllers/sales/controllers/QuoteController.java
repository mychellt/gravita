package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.CreateQuoteRequest;
import br.gravita.adapters.inbound.controllers.sales.dtos.QuoteResponse;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.CreateQuoteUseCase;
import br.gravita.core.ports.inbound.sales.QuoteView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sales/quotes")
public class QuoteController {

	private final CreateQuoteUseCase createQuoteUseCase;

	@PostMapping
	public ResponseEntity<QuoteResponse> create(@Valid @RequestBody CreateQuoteRequest request) {
		QuoteView quote = createQuoteUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/sales/quotes/" + quote.id()))
				.body(QuoteResponse.from(quote));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
