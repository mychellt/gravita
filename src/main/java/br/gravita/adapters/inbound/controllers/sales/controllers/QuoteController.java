package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.CreateQuoteRequest;
import br.gravita.adapters.inbound.controllers.sales.dtos.QuoteResponse;
import br.gravita.adapters.inbound.controllers.sales.dtos.SalesOrderResponse;
import br.gravita.adapters.inbound.controllers.sales.dtos.SendQuoteRequest;
import br.gravita.core.domain.sales.QuoteNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.ConvertQuoteToOrderCommand;
import br.gravita.core.ports.inbound.sales.ConvertQuoteToOrderUseCase;
import br.gravita.core.ports.inbound.sales.CreateQuoteUseCase;
import br.gravita.core.ports.inbound.sales.QuoteView;
import br.gravita.core.ports.inbound.sales.SalesOrderView;
import br.gravita.core.ports.inbound.sales.SendQuoteUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sales/quotes")
public class QuoteController {

	private final CreateQuoteUseCase createQuoteUseCase;
	private final SendQuoteUseCase sendQuoteUseCase;
	private final ConvertQuoteToOrderUseCase convertQuoteToOrderUseCase;

	@PostMapping
	public ResponseEntity<QuoteResponse> create(@Valid @RequestBody CreateQuoteRequest request) {
		QuoteView quote = createQuoteUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/sales/quotes/" + quote.id()))
				.body(QuoteResponse.from(quote));
	}

	@PostMapping("/{id}/send")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void send(@PathVariable UUID id, @Valid @RequestBody SendQuoteRequest request) {
		sendQuoteUseCase.execute(request.toCommand(id));
	}

	@PostMapping("/{id}/convert")
	public ResponseEntity<SalesOrderResponse> convert(@PathVariable UUID id) {
		SalesOrderView order = convertQuoteToOrderUseCase.execute(new ConvertQuoteToOrderCommand(id));
		return ResponseEntity.created(URI.create("/api/sales/orders/" + order.id()))
				.body(SalesOrderResponse.from(order));
	}

	@ExceptionHandler(QuoteNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleQuoteNotFoundException(QuoteNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
