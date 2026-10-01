package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseRequestJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseRequestPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.purchasing.PurchaseRequestJpaRepository;
import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestRepositoryAdapterTest {

	@Mock
	private PurchaseRequestJpaRepository repository;

	@Mock
	private PurchaseRequestPersistenceMapper mapper;

	@InjectMocks
	private PurchaseRequestRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new purchase request marking its entity as new")
	void shouldSaveNewPurchaseRequest() {
		final PurchaseRequest purchaseRequest = buildPurchaseRequest(UUID.randomUUID());
		final PurchaseRequestJpaEntity entity = buildEntity(purchaseRequest.getId());
		final PurchaseRequestJpaEntity saved = buildEntity(purchaseRequest.getId());
		when(mapper.map(purchaseRequest)).thenReturn(entity);
		when(repository.existsById(purchaseRequest.getId().value())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(purchaseRequest);

		final PurchaseRequest result = adapter.save(purchaseRequest);

		assertThat(result).isSameAs(purchaseRequest);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Saves an existing purchase request marking its entity as not new")
	void shouldSaveExistingPurchaseRequestAsNotNew() {
		final PurchaseRequest purchaseRequest = buildPurchaseRequest(UUID.randomUUID());
		final PurchaseRequestJpaEntity entity = buildEntity(purchaseRequest.getId());
		when(mapper.map(purchaseRequest)).thenReturn(entity);
		when(repository.existsById(purchaseRequest.getId().value())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(purchaseRequest);

		adapter.save(purchaseRequest);

		assertThat(entity.isNew()).isFalse();
	}

	@Test
	@DisplayName("Finds a purchase request by id")
	void shouldFindPurchaseRequestById() {
		final PurchaseRequest purchaseRequest = buildPurchaseRequest(UUID.randomUUID());
		final PurchaseRequestJpaEntity entity = buildEntity(purchaseRequest.getId());
		when(repository.findById(purchaseRequest.getId().value())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(purchaseRequest);

		final Optional<PurchaseRequest> result = adapter.findById(purchaseRequest.getId());

		assertThat(result).contains(purchaseRequest);
		verify(repository).findById(purchaseRequest.getId().value());
	}

	@Test
	@DisplayName("Returns true when an open request exists for the origin and product")
	void returnsTrueWhenAnOpenRequestExistsForTheOriginAndProduct() {
		final UUID productId = UUID.randomUUID();
		when(repository.existsByOriginAndStatusAndItems_ProductId(PurchaseRequestOrigin.MIN_STOCK_TRIGGER,
				PurchaseRequestStatus.OPEN, productId)).thenReturn(true);

		assertThat(adapter.existsOpenByOriginAndProductId(PurchaseRequestOrigin.MIN_STOCK_TRIGGER, productId)).isTrue();
		verify(repository).existsByOriginAndStatusAndItems_ProductId(PurchaseRequestOrigin.MIN_STOCK_TRIGGER,
				PurchaseRequestStatus.OPEN, productId);
	}

	@Test
	@DisplayName("Returns false when no open request exists for the origin and product")
	void returnsFalseWhenNoOpenRequestExistsForTheOriginAndProduct() {
		final UUID productId = UUID.randomUUID();
		when(repository.existsByOriginAndStatusAndItems_ProductId(PurchaseRequestOrigin.MIN_STOCK_TRIGGER,
				PurchaseRequestStatus.OPEN, productId)).thenReturn(false);

		assertThat(adapter.existsOpenByOriginAndProductId(PurchaseRequestOrigin.MIN_STOCK_TRIGGER, productId)).isFalse();
	}

	private PurchaseRequestJpaEntity buildEntity(final PurchaseRequestId id) {
		return PurchaseRequestJpaEntity.builder().id(id.value()).build();
	}

	private PurchaseRequest buildPurchaseRequest(final UUID productId) {
		return PurchaseRequest.open(PurchaseRequestId.of(UUID.randomUUID()), PurchaseRequestOrigin.MIN_STOCK_TRIGGER,
				List.of(new PurchaseRequestItem(productId, BigDecimal.TEN)), null);
	}
}
