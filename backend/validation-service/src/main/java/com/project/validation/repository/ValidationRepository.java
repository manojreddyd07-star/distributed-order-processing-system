package com.project.validation.repository;

import com.project.validation.entity.ValidationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidationRepository extends JpaRepository<ValidationEntity, Long> {
}
