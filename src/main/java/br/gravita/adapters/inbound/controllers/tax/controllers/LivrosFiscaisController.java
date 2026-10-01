package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.LivrosFiscaisResponse;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.ports.inbound.tax.GenerateLivrosFiscaisCommand;
import br.gravita.core.ports.inbound.tax.GenerateLivrosFiscaisUseCase;
import java.time.YearMonth;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/livros-fiscais")
public class LivrosFiscaisController {

	private final GenerateLivrosFiscaisUseCase generateLivrosFiscaisUseCase;

	/** {@code period} is a month as {@code yyyy-MM}. */
	@GetMapping
	public ResponseEntity<LivrosFiscaisResponse> generate(@RequestParam UUID companyId,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth period) {
		return ResponseEntity.ok(LivrosFiscaisResponse.from(generateLivrosFiscaisUseCase
				.execute(new GenerateLivrosFiscaisCommand(CompanyId.of(companyId), period))));
	}
}
