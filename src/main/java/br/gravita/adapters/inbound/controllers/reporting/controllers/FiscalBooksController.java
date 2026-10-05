package br.gravita.adapters.inbound.controllers.reporting.controllers;

import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.FiscalBooks;
import br.gravita.core.ports.inbound.reporting.FiscalBooksQuery;
import br.gravita.core.ports.inbound.reporting.GetFiscalBooksUseCase;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reports/fiscal-books")
public class FiscalBooksController {

	private final GetFiscalBooksUseCase getFiscalBooksUseCase;

	/**
	 * {@code period} is a month as {@code yyyy-MM}. The rendered {@code pdf} and {@code txt} come back base64-encoded
	 * in the JSON body, next to the structured books they were laid out from.
	 */
	@GetMapping
	public ResponseEntity<FiscalBooks> get(@AuthenticatedUser final UserId callerId,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") final YearMonth period) {
		return ResponseEntity.ok(getFiscalBooksUseCase.execute(new FiscalBooksQuery(callerId, period)));
	}
}
