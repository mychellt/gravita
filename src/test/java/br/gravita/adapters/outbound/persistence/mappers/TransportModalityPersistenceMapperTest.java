package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.TransportModalityJpaEntity;
import br.gravita.core.domain.TransportModalityDomain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransportModalityPersistenceMapperTest {

	private final TransportModalityPersistenceMapper mapper = Mappers.getMapper(TransportModalityPersistenceMapper.class);

	@Test
	@DisplayName("Maps the active flag and audit timestamps both ways")
	void shouldRoundTripActiveFlag() {
		final LocalDateTime created = LocalDateTime.of(2026, 10, 14, 10, 0);
		final TransportModalityDomain domain = TransportModalityDomain.builder()
				.id(UUID.randomUUID()).name("Sem transporte").code("NONE").active(false).createdAt(created).build();

		final TransportModalityJpaEntity entity = mapper.map(domain);
		assertThat(entity.isActive()).isFalse();
		assertThat(entity.getCode()).isEqualTo("NONE");
		assertThat(entity.getCreatedAt()).isEqualTo(created);

		final TransportModalityDomain back = mapper.map(entity);
		assertThat(back.getActive()).isFalse();
		assertThat(back.getName()).isEqualTo("Sem transporte");
	}

	@Test
	@DisplayName("A domain without an active flag maps to an active entity")
	void shouldDefaultToActive() {
		final TransportModalityDomain domain = TransportModalityDomain.builder().id(UUID.randomUUID()).name("CIF").code("CIF").build();

		assertThat(mapper.map(domain).isActive()).isTrue();
	}
}
