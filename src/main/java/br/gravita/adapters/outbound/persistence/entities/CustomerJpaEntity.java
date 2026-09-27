package br.gravita.adapters.outbound.persistence.entities;

import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.shared.PersonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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
public class CustomerJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

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

	@Version
	@Column(nullable = false)
	private Long version;

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
