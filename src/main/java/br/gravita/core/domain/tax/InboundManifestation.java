package br.gravita.core.domain.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * UC-M2-07: the recipient's manifestation on an inbound NFe - confirming the
 * operation, declaring it unknown, or declaring it wasn't performed -
 * forwarded to and accepted by SEFAZ. Works by {@code accessKey} alone
 * (doc's AC3): {@link #inboundNfeId} is only set when a matching
 * {@link InboundNfe} row happens to already exist locally, never required.
 */
@Getter
public final class InboundManifestation {

	private static final Pattern ACCESS_KEY_PATTERN = Pattern.compile("\\d{44}");

	private final InboundManifestationId id;
	private final String accessKey;
	private final ManifestationType type;
	private final InboundNfeId inboundNfeId;
	private final String sefazProtocol;
	private final Instant manifestedAt;

	private InboundManifestation(InboundManifestationId id, String accessKey, ManifestationType type,
			InboundNfeId inboundNfeId, String sefazProtocol, Instant manifestedAt) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.accessKey = requireAccessKey(accessKey);
		this.type = Objects.requireNonNull(type, "type is required");
		this.inboundNfeId = inboundNfeId;
		this.sefazProtocol = requireNonBlank(sefazProtocol, "sefazProtocol");
		this.manifestedAt = Objects.requireNonNull(manifestedAt, "manifestedAt is required");
	}

	public static InboundManifestation of(InboundManifestationId id, String accessKey, ManifestationType type,
			InboundNfeId inboundNfeId, String sefazProtocol, Instant manifestedAt) {
		return new InboundManifestation(id, accessKey, type, inboundNfeId, sefazProtocol, manifestedAt);
	}

	public Optional<InboundNfeId> getInboundNfeId() {
		return Optional.ofNullable(inboundNfeId);
	}

	private static String requireAccessKey(String accessKey) {
		if (accessKey == null || !ACCESS_KEY_PATTERN.matcher(accessKey).matches()) {
			throw new BusinessRuleException("NFe access key must be 44 digits: " + accessKey);
		}
		return accessKey;
	}

	private static String requireNonBlank(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(field + " is required");
		}
		return value;
	}
}
