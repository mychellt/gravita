package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.ProductSearchResultResponse;
import br.gravita.core.ports.inbound.tax.SearchProductForSaleUseCase;
import br.gravita.core.ports.inbound.tax.SearchProductQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/pdv/products")
public class ProductSearchController {

	private final SearchProductForSaleUseCase searchProductForSaleUseCase;

	@GetMapping("/search")
	public ResponseEntity<List<ProductSearchResultResponse>> search(@RequestParam("q") String q,
			@RequestParam(required = false) UUID customerId) {
		List<ProductSearchResultResponse> results = searchProductForSaleUseCase.execute(new SearchProductQuery(q, customerId))
				.stream()
				.map(ProductSearchResultResponse::from)
				.toList();
		return ResponseEntity.ok(results);
	}
}
