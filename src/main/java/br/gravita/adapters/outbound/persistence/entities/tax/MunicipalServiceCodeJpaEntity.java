package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "municipal_service_codes",
		uniqueConstraints = @UniqueConstraint(name = "uk_municipal_service_codes",
				columnNames = { "municipality_ibge", "service_code" }))
public class MunicipalServiceCodeJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "municipality_ibge", nullable = false, length = 7)
	private String municipalityIbge;

	@Column(name = "service_code", nullable = false, length = 5)
	private String serviceCode;
}
