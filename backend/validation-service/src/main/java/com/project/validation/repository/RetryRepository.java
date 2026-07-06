package com.project.validation.repository;

import com.project.validation.entity.RetryRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetryRepository extends JpaRepository<RetryRecordEntity, Long> {
    
    /**
     * Find retry record by retry ID
     */
    Optional<RetryRecordEntity> findByRetryId(String retryId);
    
    /**
     * Find all retry records by original event ID
     */
    List<RetryRecordEntity> findByOriginalEventId(String originalEventId);
    
    /**
     * Find retry records by status
     */
    List<RetryRecordEntity> findByRetryStatus(String retryStatus);
    
    /**
     * Find retry records that are ready for retry (next retry time has passed)
     */
    @Query("SELECT r FROM RetryRecordEntity r WHERE r.retryStatus = 'PENDING' AND r.nextRetryTime <= :currentTime")
    List<RetryRecordEntity> findPendingRetries(LocalDateTime currentTime);
    
    /**
     * Find all retry records ordered by created time desc
     */
    List<RetryRecordEntity> findAllByOrderByCreatedAtDesc();
}
