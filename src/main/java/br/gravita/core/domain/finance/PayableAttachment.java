package br.gravita.core.domain.finance;

import java.util.Objects;

/** A supporting document (boleto, NF, receipt) linked to a payable; {@code storageRef} locates the stored bytes. */
public record PayableAttachment(String storageRef, String fileName, String contentType, long sizeBytes) {

	public PayableAttachment {
		Objects.requireNonNull(storageRef, "storageRef is required");
		Objects.requireNonNull(fileName, "fileName is required");
		Objects.requireNonNull(contentType, "contentType is required");
	}
}
