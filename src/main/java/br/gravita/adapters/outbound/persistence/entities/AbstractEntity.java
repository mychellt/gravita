package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.util.Date;

@Data
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
public abstract class AbstractEntity<K>  implements Persistable<K> {

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Date createdAt;

	@UpdateTimestamp
	@Column(name = "modified_at", nullable = false)
	private Date modifiedAt;

	@Column(nullable = false)
	@Builder.Default
	private boolean active = true;

	@Transient
	@Builder.Default
	private boolean isNew = true;

	@Override
	public boolean isNew() {
		return isNew;
	}

	// Hibernate hydrates loaded/inserted rows via reflection, bypassing this
	// field's @Builder.Default, so a fetched or just-inserted row would
	// otherwise still report isNew()==true (its default) — which makes
	// SimpleJpaRepository#delete/deleteById silently no-op, since it skips
	// the actual removal whenever isNew() is true.
	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}

}
