package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.*;
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
public abstract class AbstractEntity<K> implements Persistable<K> {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private Date modifiedAt;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

}
