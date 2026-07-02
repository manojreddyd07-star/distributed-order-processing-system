package com.project.validation.repository;

import com.project.validation.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecordEntity, Long> {
    
    /**
     * Find an idempotency record by event ID
     * @param eventId The unique event ID
     * @return Optional containing the idempotency record if found
     */
    Optional<IdempotencyRecordEntity> findByEventId(String eventId);
    
    /**
     * Check if an event has already been processed
     * @param eventId The unique event ID
     * @return true if the event exists in the database
     */
    boolean existsByEventId(String eventId);
}
