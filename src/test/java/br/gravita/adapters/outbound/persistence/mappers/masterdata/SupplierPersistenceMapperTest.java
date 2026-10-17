package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierJpaEntity;
import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.Document;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class SupplierPersistenceMapperTest {

	private final SupplierPersistenceMapper mapper = Mappers.getMapper(SupplierPersistenceMapper.class);

	@Test
	@DisplayName("Maps the person fields and the fiscal profile both ways")
	void shouldRoundTripThePersonFieldsAndTheFiscalProfile() {
		final Supplier supplier = new Supplier(SupplierId.of(UUID.randomUUID()), Document.cnpj("11222333000181"), "Fornecedor Alfa",
				List.of(new Address("Rua A", "10", null, "Centro", "Recife", "PE", "50000000")), List.of(), null, null, 7, "1102",
				"123456789", "4455", "4751201", true);

		final SupplierJpaEntity entity = mapper.map(supplier);

		assertThat(entity.getId()).isEqualTo(supplier.getId().value());
		assertThat(entity.getName()).isEqualTo("Fornecedor Alfa");
		assertThat(entity.getDocument()).isEqualTo("11222333000181");
		assertThat(entity.getIe()).isEqualTo("123456789");
		assertThat(entity.getSimplesOptante()).isTrue();

		final Supplier back = mapper.map(entity);

		assertThat(back.getName()).isEqualTo("Fornecedor Alfa");
		assertThat(back.getDocument()).isEqualTo(supplier.getDocument());
		assertThat(back.getIe()).isEqualTo("123456789");
		assertThat(back.getIm()).isEqualTo("4455");
		assertThat(back.getCnae()).isEqualTo("4751201");
		assertThat(back.getSimplesOptante()).isTrue();
		assertThat(back.getAverageLeadTimeDays()).isEqualTo(7);
	}
}
