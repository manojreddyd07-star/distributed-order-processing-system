package com.project.fulfillment.repository;

import com.project.fulfillment.entity.RetryRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetryRepository extends JpaRepository<RetryRecordEntity, Long> {
    Optional<RetryRecordEntity> findByRetryId(String retryId);
    List<RetryRecordEntity> findByOriginalEventId(String originalEventId);
    List<RetryRecordEntity> findByRetryStatus(String retryStatus);
    @Query("SELECT r FROM RetryRecordEntity r WHERE r.retryStatus = 'PENDING' AND r.nextRetryTime <= :currentTime")
    List<RetryRecordEntity> findPendingRetries(LocalDateTime currentTime);
    List<RetryRecordEntity> findAllByOrderByCreatedAtDesc();
}
