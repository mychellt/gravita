package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.shared.PersonType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "suppliers")
public class SupplierJpaEntity implements Persistable<UUID> {

	@Id
	private UUID id;

	// id is always application-assigned (RegisterSupplierService); the repository
	// adapter sets this from existsById before save so Spring Data picks
	// persist() over merge() for a row that doesn't exist yet.
	@Transient
	@Builder.Default
	private boolean isNew = true;

	@Override
	public boolean isNew() {
		return isNew;
	}

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

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "modified_at", nullable = false)
	private LocalDateTime modifiedAt;
}
