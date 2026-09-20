package br.gravita.adapters.outbound.persistence.entities;

import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.shared.PersonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customers")
public class CustomerJpaEntity extends AbstractEntity<UUID> implements Persistable<UUID> {

	@Id
	private UUID id;

	// id is always application-assigned (CustomerRegistrationAdapter); the
	// repository adapter sets this from existsById before save so Spring Data
	// picks persist() over merge() for a row that doesn't exist yet.
	@Transient
	@Builder.Default
	private boolean isNew = true;

	@Override
	public boolean isNew() {
		return isNew;
	}

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private PersonType personType;

	@Column(nullable = false, unique = true)
	private String document;

	private String email;

	@Column(length = 20)
	@Enumerated(EnumType.STRING)
	private IeIndicator ieIndicator;

	private Boolean finalConsumer;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal creditLimit;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal currentBalance;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private CustomerStatus status;

	@ElementCollection
	@CollectionTable(name = "customer_addresses", joinColumns = @JoinColumn(name = "customer_id"))
	private List<CustomerAddressEmbeddable> addresses;

	@ElementCollection
	@CollectionTable(name = "customer_contacts", joinColumns = @JoinColumn(name = "customer_id"))
	private List<CustomerContactEmbeddable> contacts;

	@ElementCollection
	@CollectionTable(name = "customer_price_tables", joinColumns = @JoinColumn(name = "customer_id"))
	private List<CustomerPriceTableEmbeddable> priceTables;
}
