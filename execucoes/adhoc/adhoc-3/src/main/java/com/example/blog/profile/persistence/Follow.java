package com.example.blog.profile.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Relação "usuário A segue usuário B" (tabela {@code user_follow}).
 *
 * <p>As escritas são feitas por {@link FollowRepository} com SQL idempotente; a entidade
 * existe para consultas e para a validação do mapeamento contra o esquema.
 */
@Entity
@Table(name = "user_follow")
public class Follow {

    @EmbeddedId
    private FollowId id;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Follow() {
        // exigido pelo JPA
    }

    public FollowId getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Follow that && id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Follow.class.hashCode();
    }
}
