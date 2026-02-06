package com.quantlab.backend.repository;

import com.quantlab.backend.entity.CapitalAllocationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CapitalAllocationSnapshotRepository extends JpaRepository<CapitalAllocationSnapshot, Long> {

    @Query("SELECT s FROM CapitalAllocationSnapshot s WHERE s.runDate BETWEEN :startDate AND :endDate ORDER BY s.runDate DESC")
    List<CapitalAllocationSnapshot> findByRunDateBetweenOrderByRunDateDesc(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}