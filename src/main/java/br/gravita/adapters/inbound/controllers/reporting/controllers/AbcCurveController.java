package br.gravita.adapters.inbound.controllers.reporting.controllers;

import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.AbcCurveEntry;
import br.gravita.core.ports.inbound.reporting.AbcCurveQuery;
import br.gravita.core.ports.inbound.reporting.AbcCurveType;
import br.gravita.core.ports.inbound.reporting.GetAbcCurveUseCase;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reports/abc-curve")
public class AbcCurveController {

	private final GetAbcCurveUseCase getAbcCurveUseCase;

	/** {@code type} is {@code product} or {@code customer}; {@code period} is a month as {@code yyyy-MM}. */
	@GetMapping
	public ResponseEntity<List<AbcCurveEntry>> get(@AuthenticatedUser final UserId callerId, @RequestParam final String type,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") final YearMonth period,
			@RequestParam(required = false) final UUID companyId) {
		return ResponseEntity.ok(getAbcCurveUseCase.execute(new AbcCurveQuery(callerId, parse(type), period, companyId)));
	}

	private AbcCurveType parse(final String type) {
		try {
			return AbcCurveType.valueOf(type.trim().toUpperCase(Locale.ROOT));
		} catch (final IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be product or customer");
		}
	}
}
