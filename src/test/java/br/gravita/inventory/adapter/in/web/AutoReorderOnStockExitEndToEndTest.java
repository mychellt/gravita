package br.gravita.inventory.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseRequestJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockBalanceJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.purchasing.PurchaseRequestJpaRepository;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AutoReorderOnStockExitEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepositoryPort productRepositoryPort;

	@Autowired
	private StockBalanceJpaRepository stockBalanceJpaRepository;

	@Autowired
	private PurchaseRequestJpaRepository purchaseRequestJpaRepository;

	@Test
	@DisplayName("AC1: A stock exit that drops available stock to or below the reorder point opens a minimum-stock purchase request")
	void ac1_openingAMinStockTriggerRequestWhenAnExitDropsAvailableToOrBelowTheReorderPoint() throws Exception {
		UUID productId = seedProduct("10", "100", "20");
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "35");

		exit(productId, warehouseId, "20");

		List<PurchaseRequestJpaEntity> requests = matchingRequests(productId);
		assertThat(requests).hasSize(1);
		assertThat(requests.get(0).getOrigin()).isEqualTo(PurchaseRequestOrigin.MIN_STOCK_TRIGGER);
		assertThat(requests.get(0).getStatus()).isEqualTo(PurchaseRequestStatus.OPEN);
		assertThat(requests.get(0).getRequestedBy()).isNull();
		assertThat(requests.get(0).getItems().get(0).getQuantity()).isEqualByComparingTo("85");
	}

	@Test
	@DisplayName("AC2: A further stock drop does not open a second purchase request while one is already open")
	void ac2_aFurtherDropDoesNotOpenASecondRequestWhileOneIsAlreadyOpen() throws Exception {
		UUID productId = seedProduct("10", "100", "20");
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "35");

		exit(productId, warehouseId, "20");
		assertThat(matchingRequests(productId)).hasSize(1);

		exit(productId, warehouseId, "5");

		assertThat(matchingRequests(productId)).hasSize(1);
	}

	@Test
	@DisplayName("No purchase request is opened when available stock stays above the reorder point after an exit")
	void doesNotOpenARequestWhenAvailableStaysAboveTheReorderPoint() throws Exception {
		UUID productId = seedProduct("10", "100", "20");
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "100");

		exit(productId, warehouseId, "10");

		assertThat(matchingRequests(productId)).isEmpty();
	}

	private void exit(UUID productId, UUID warehouseId, String quantity) throws Exception {
		mockMvc.perform(post("/api/inventory/movements/exit")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": "%s",
								  "warehouseId": "%s",
								  "quantity": %s,
								  "originReference": "SALE:1",
								  "user": "%s"
								}
								""".formatted(productId, warehouseId, quantity, UUID.randomUUID())))
				.andExpect(status().isCreated());
	}

	private List<PurchaseRequestJpaEntity> matchingRequests(UUID productId) {
		return purchaseRequestJpaRepository.findAll().stream()
				.filter(entity -> entity.getItems().stream().anyMatch(item -> item.getProductId().equals(productId)))
				.toList();
	}

	private UUID seedProduct(String minimum, String maximum, String reorderPoint) {
		UUID id = UUID.randomUUID();
		productRepositoryPort.save(ProductDomain.builder()
				.id(id)
				.internalCode("SKU-" + id)
				.type(ProductType.SIMPLE)
				.status(ProductStatus.ACTIVE)
				.stock(new StockParametersDomain(new BigDecimal(minimum), new BigDecimal(maximum),
						new BigDecimal(reorderPoint)))
				.build());
		return id;
	}

	private void seedBalance(UUID productId, UUID warehouseId, String onHand) {
		stockBalanceJpaRepository.save(StockBalanceJpaEntity.builder()
				.id(UUID.randomUUID())
				.productId(productId)
				.warehouseId(warehouseId)
				.onHand(new BigDecimal(onHand))
				.reserved(BigDecimal.ZERO)
				.inTransit(BigDecimal.ZERO)
				.averageCost(new BigDecimal("10.00"))
				.build());
	}
}
