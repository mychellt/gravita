package br.gravita.core.usercases.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.ProductPurchase;
import br.gravita.core.ports.inbound.tax.GetProductPurchaseHistoryQuery;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.ProductPurchaseHistoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetProductPurchaseHistoryServiceTest {

	@Mock
	private ProductPurchaseHistoryPort productPurchaseHistoryPort;

	@Mock
	private UserRepositoryPort userRepositoryPort;

	private GetProductPurchaseHistoryService service;

	private final UserId callerId = UserId.generate();
	private final UUID companyUuid = UUID.randomUUID();
	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new GetProductPurchaseHistoryService(productPurchaseHistoryPort, new CallerCompanyResolver(userRepositoryPort));
	}

	@Test
	@DisplayName("Returns the purchases of the product inside the caller's own company")
	void returnsThePurchasesOfTheCallersCompany() {
		final ProductPurchase purchase = new ProductPurchase(UUID.randomUUID(), "1", "109887", "19117785000105", "Fornecedor A",
				Instant.parse("2026-01-10T12:00:00Z"), BigDecimal.ONE, new BigDecimal("6998.89"));
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyUuid).build()));
		when(productPurchaseHistoryPort.findByProduct(CompanyId.of(companyUuid), productId)).thenReturn(List.of(purchase));

		final List<ProductPurchase> history = service.execute(new GetProductPurchaseHistoryQuery(productId, callerId));

		assertThat(history).containsExactly(purchase);
		verify(productPurchaseHistoryPort).findByProduct(CompanyId.of(companyUuid), productId);
	}

	@Test
	@DisplayName("Returns no purchases when the caller has no company")
	void returnsNothingWhenTheCallerHasNoCompany() {
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(null).build()));

		assertThat(service.execute(new GetProductPurchaseHistoryQuery(productId, callerId))).isEmpty();
	}
}
