package com.quantlab.backend.service;

import com.quantlab.backend.dto.*;
import com.quantlab.backend.entity.*;
import com.quantlab.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CapitalAllocationService {

    private final TradeSignalRepository tradeSignalRepository;
    private final CapitalAllocationSnapshotRepository snapshotRepository;
    private final CapitalAllocationPositionRepository positionRepository;

    @Autowired
    public CapitalAllocationService(TradeSignalRepository tradeSignalRepository,
                                   CapitalAllocationSnapshotRepository snapshotRepository,
                                   CapitalAllocationPositionRepository positionRepository) {
        this.tradeSignalRepository = tradeSignalRepository;
        this.snapshotRepository = snapshotRepository;
        this.positionRepository = positionRepository;
    }

    /**
     * Simulates capital allocation for the top N ranked signals
     */
    @Transactional
    public com.quantlab.backend.dto.CapitalAllocationSnapshot simulateAllocation(AllocationRequest request) {
        // Calculate risk per trade
        BigDecimal riskPerTrade = request.getTotalCapital().multiply(
            BigDecimal.valueOf(request.getRiskPerTradePct() / 100.0)
        );

        // Fetch top ranked signals
        List<TradeSignal> topSignals = tradeSignalRepository
            .findTopPendingSignalsByRankScore(request.getDate(), request.getMaxOpenTrades());

        BigDecimal availableCapital = request.getTotalCapital();
        List<AllocationPosition> positions = new ArrayList<>();
        double totalExpectedR = 0.0;

        for (TradeSignal signal : topSignals) {
            // Calculate position size using risk-based formula
            BigDecimal qty = calculatePositionSize(
                signal,
                riskPerTrade,
                availableCapital
            );

            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // Skip if insufficient capital or invalid setup
            }

            BigDecimal capitalUsed = qty.multiply(signal.getEntryPrice());
            BigDecimal entry = signal.getEntryPrice();
            BigDecimal stopLoss = signal.getStopLoss();
            BigDecimal target = signal.getTargetPrice();

            // Calculate expected R-multiple for this position
            double expectedR = stopLoss != null && target != null
                ? target.subtract(entry).doubleValue() / entry.subtract(stopLoss).doubleValue()
                : 0.0;

            AllocationPosition position = new AllocationPosition(
                signal.getInstrument().getSymbol(),
                qty.intValue(),
                capitalUsed,
                riskPerTrade,
                BigDecimal.valueOf(expectedR),
                capitalUsed.divide(request.getTotalCapital(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            );

            positions.add(position);
            totalExpectedR += expectedR;
            availableCapital = availableCapital.subtract(capitalUsed);

            if (availableCapital.compareTo(riskPerTrade.multiply(BigDecimal.valueOf(2))) < 0) {
                break; // Stop if insufficient capital for another position
            }
        }

        BigDecimal deployedCapital = request.getTotalCapital().subtract(availableCapital);

        // For now, create a simple DTO response without persisting
        // TODO: Implement proper persistence with position relationships
        com.quantlab.backend.dto.CapitalAllocationSnapshot dtoSnapshot = new com.quantlab.backend.dto.CapitalAllocationSnapshot();
        dtoSnapshot.setRunDate(request.getDate());
        dtoSnapshot.setTotalCapital(request.getTotalCapital());
        dtoSnapshot.setDeployedCapital(deployedCapital);
        dtoSnapshot.setFreeCash(availableCapital);
        dtoSnapshot.setExpectedRMultiple(BigDecimal.valueOf(totalExpectedR));
        dtoSnapshot.setPositions(positions);
        dtoSnapshot.setId(-1L); // Temporary ID

        return dtoSnapshot;
    }

    /**
     * Get capital allocation history for a date range
     */
    public List<com.quantlab.backend.dto.CapitalAllocationSnapshot> getHistory(LocalDate startDate, LocalDate endDate) {
        // For now, return empty list as we're not persisting data yet
        return List.of();
    }

    private BigDecimal calculatePositionSize(
        TradeSignal signal,
        BigDecimal riskAmount,
        BigDecimal availableCapital
    ) {
        BigDecimal entryPrice = signal.getEntryPrice();
        BigDecimal stopLoss = signal.getStopLoss();

        if (stopLoss == null || entryPrice.compareTo(stopLoss) <= 0) {
            return BigDecimal.ZERO; // Invalid setup
        }

        // Risk per share = Entry - StopLoss
        BigDecimal riskPerShare = entryPrice.subtract(stopLoss);

        // Position size = Risk Amount / Risk Per Share
        BigDecimal qty = riskAmount.divide(riskPerShare, 0, RoundingMode.DOWN);

        // Capital constraint
        BigDecimal capitalRequired = qty.multiply(entryPrice);
        if (capitalRequired.compareTo(availableCapital) > 0) {
            qty = availableCapital.divide(entryPrice, 0, RoundingMode.DOWN);
        }

        return qty;
    }
}