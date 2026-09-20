package br.gravita.adapters.outbound.persistence.entities.masterdata;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.shared.PersonType;
import jakarta.persistence.*;

import java.util.List;
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
@Table(name = "suppliers")
public class SupplierJpaEntity extends AbstractEntity<UUID> {
	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 20)
	private String document;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PersonType personType;

	@Column(nullable = false)
	private String name;

	@ElementCollection
	@CollectionTable(name = "supplier_addresses", joinColumns = @JoinColumn(name = "supplier_id"))
	private List<SupplierAddressEmbeddable> addresses;

	@ElementCollection
	@CollectionTable(name = "supplier_contacts", joinColumns = @JoinColumn(name = "supplier_id"))
	private List<SupplierContactEmbeddable> contacts;

	@Column(name = "bank_code", length = 10)
	private String bankCode;

	@Column(name = "bank_agency", length = 20)
	private String bankAgency;

	@Column(name = "bank_account_number", length = 30)
	private String bankAccountNumber;

	@Column(name = "pix_key")
	private String pixKey;

	@Column(name = "average_lead_time_days")
	private Integer averageLeadTimeDays;

	@Column(name = "default_purchase_cfop", length = 4)
	private String defaultPurchaseCfop;
}
