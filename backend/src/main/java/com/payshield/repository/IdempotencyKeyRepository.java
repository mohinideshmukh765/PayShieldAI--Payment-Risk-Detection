package com.payshield.repository;

import com.payshield.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository
        extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey>
    findByUserIdAndKey(UUID userId, String key);
}