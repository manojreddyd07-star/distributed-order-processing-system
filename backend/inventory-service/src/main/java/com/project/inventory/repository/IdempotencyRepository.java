package com.project.inventory.repository;

import com.project.inventory.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecordEntity, Long> {
    
    /**
     * Find an idempotency record by event ID
     * @param eventId The unique event ID
     * @return Optional containing the idempotency record if found
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "false"))
    @Query("SELECT i FROM IdempotencyRecordEntity i WHERE i.eventId = :eventId")
    Optional<IdempotencyRecordEntity> findByEventId(String eventId);
    
    /**
     * Check if an event has already been processed (optimized query)
     * @param eventId The unique event ID
     * @return true if the event exists in the database
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM IdempotencyRecordEntity i WHERE i.eventId = :eventId")
    boolean existsByEventId(String eventId);
}
