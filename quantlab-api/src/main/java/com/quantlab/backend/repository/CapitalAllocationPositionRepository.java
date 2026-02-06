package com.quantlab.backend.repository;

import com.quantlab.backend.entity.CapitalAllocationPosition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapitalAllocationPositionRepository extends JpaRepository<CapitalAllocationPosition, Long> {
}