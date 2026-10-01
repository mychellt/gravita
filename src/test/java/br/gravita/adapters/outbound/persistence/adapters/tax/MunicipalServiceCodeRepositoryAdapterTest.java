package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.repositories.tax.MunicipalServiceCodeJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MunicipalServiceCodeRepositoryAdapterTest {

	@Mock
	private MunicipalServiceCodeJpaRepository repository;

	@InjectMocks
	private MunicipalServiceCodeRepositoryAdapter adapter;

	@Test
	@DisplayName("Reports whether a municipality has an active service code list")
	void hasServiceCodeListReportsWhetherTheMunicipalityHasAList() {
		when(repository.existsByMunicipalityIbgeAndActiveTrue("3550308")).thenReturn(true);
		when(repository.existsByMunicipalityIbgeAndActiveTrue("3304557")).thenReturn(false);

		assertThat(adapter.hasServiceCodeList("3550308")).isTrue();
		assertThat(adapter.hasServiceCodeList("3304557")).isFalse();
		verify(repository).existsByMunicipalityIbgeAndActiveTrue("3550308");
	}

	@Test
	@DisplayName("Reports whether a service code is listed for a municipality")
	void existsByMunicipalityAndServiceCodeReportsWhetherTheCodeIsListed() {
		when(repository.existsByMunicipalityIbgeAndServiceCodeAndActiveTrue("3550308", "01.05")).thenReturn(true);
		when(repository.existsByMunicipalityIbgeAndServiceCodeAndActiveTrue("3550308", "02.01")).thenReturn(false);

		assertThat(adapter.existsByMunicipalityAndServiceCode("3550308", "01.05")).isTrue();
		assertThat(adapter.existsByMunicipalityAndServiceCode("3550308", "02.01")).isFalse();
	}
}
