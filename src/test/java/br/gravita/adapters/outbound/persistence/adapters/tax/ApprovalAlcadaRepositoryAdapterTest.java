package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ApprovalAlcadaJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.ApprovalAlcadaPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.ApprovalAlcadaJpaRepository;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.ProfileReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalAlcadaRepositoryAdapterTest {

	@Mock
	private ApprovalAlcadaJpaRepository repository;

	@Mock
	private ApprovalAlcadaPersistenceMapper mapper;

	@InjectMocks
	private ApprovalAlcadaRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves an approval alcada")
	void shouldSaveAlcada() {
		final ApprovalAlcada alcada = buildAlcada(ApprovalModule.PURCHASING);
		final ApprovalAlcadaJpaEntity entity = ApprovalAlcadaJpaEntity.builder().build();
		final ApprovalAlcadaJpaEntity saved = ApprovalAlcadaJpaEntity.builder().build();
		when(mapper.map(alcada)).thenReturn(entity);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(alcada);

		final ApprovalAlcada result = adapter.save(alcada);

		assertThat(result).isSameAs(alcada);
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Finds an approval alcada by module")
	void shouldFindAlcadaByModule() {
		final ApprovalAlcada alcada = buildAlcada(ApprovalModule.PURCHASING);
		final ApprovalAlcadaJpaEntity entity = ApprovalAlcadaJpaEntity.builder().build();
		when(repository.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(alcada);

		final Optional<ApprovalAlcada> result = adapter.findByModule(ApprovalModule.PURCHASING);

		assertThat(result).contains(alcada);
		verify(repository).findByModule(ApprovalModule.PURCHASING);
	}

	@Test
	@DisplayName("Returns empty when the module has no alcada configured")
	void shouldReturnEmptyWhenModuleHasNoAlcadaConfigured() {
		when(repository.findByModule(ApprovalModule.SALES)).thenReturn(Optional.empty());

		assertThat(adapter.findByModule(ApprovalModule.SALES)).isEmpty();
	}

	private ApprovalAlcada buildAlcada(final ApprovalModule module) {
		return ApprovalAlcada.configure(module, new BigDecimal("5000.00"), null,
				new ProfileReference(UUID.randomUUID(), "Purchasing Manager"));
	}
}
