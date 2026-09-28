package br.gravita.adapters.outbound.persistence.entities.finance;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "receivables")
public class ReceivableJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReceivableOrigin origin;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column
	private Integer installments;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReceivableStatus status;

	@Column(name = "origin_document_ref")
	private UUID originDocumentRef;

	@Column(name = "installment_number")
	private Integer installmentNumber;

	@Column(name = "company_id")
	private UUID companyId;

	@Column(name = "branch_id")
	private UUID branchId;

	@Column(name = "bank_account_id")
	private UUID bankAccountId;
}
