package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Persistable;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profiles")
public class ProfileJpaEntity extends AbstractEntity<UUID> implements Persistable<UUID> {
	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String name;

	@ElementCollection
	@CollectionTable(name = "profile_permissions", joinColumns = @JoinColumn(name = "profile_id"))
	private List<PermissionJpaEntity> permissions;

	// id is always caller-assigned (URL path variable); repository adapter sets this from existsById before save
	@Transient
	@Builder.Default
	private boolean isNew = true;

	@Override
	public boolean isNew() {
		return isNew;
	}
}
