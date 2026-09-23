package br.gravita.adapters.outbound.persistence.entities.purchasing;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentTermEmbeddable {

	@Column(name = "amount", nullable = false)
	private BigDecimal amount;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;
}
