package br.gravita.adapters.outbound.persistence.entities.tax;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
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
public class NfeCorrectionLetterEmbeddable {

	@Column(name = "sequence_number", nullable = false)
	private Integer sequenceNumber;

	@Column(name = "text", nullable = false)
	private String text;

	@Column(name = "protocol", nullable = false)
	private String protocol;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;
}
