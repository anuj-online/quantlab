package com.quantlab.backend.repository;

import com.quantlab.backend.entity.Candle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for Candle entity.
 */
@Repository
public interface CandleRepository extends JpaRepository<Candle, Long> {

    /**
     * Find all candles for an instrument within a date range, ordered by trade date ascending.
     *
     * @param instrumentId the instrument ID
     * @param startDate    the start date (inclusive)
     * @param endDate      the end date (inclusive)
     * @return list of candles within the date range
     */
    List<Candle> findByInstrumentIdAndTradeDateBetweenOrderByTradeDateAsc(
            Long instrumentId, LocalDate startDate, LocalDate endDate);

    /**
     * Find all candles for an instrument ordered by trade date ascending.
     *
     * @param instrumentId the instrument ID
     * @return list of all candles for the instrument
     */
    List<Candle> findByInstrumentIdOrderByTradeDateAsc(Long instrumentId);

    /**
     * Find a candle by instrument and trade date.
     *
     * @param instrumentId the instrument ID
     * @param tradeDate    the trade date
     * @return the candle if found
     */
    Candle findByInstrumentIdAndTradeDate(Long instrumentId, LocalDate tradeDate);

    /**
     * Check if a candle exists for the given instrument and trade date.
     * Used for idempotent inserts during Bhavcopy loading.
     *
     * @param instrumentId the instrument ID
     * @param tradeDate    the trade date
     * @return true if candle exists, false otherwise
     */
    boolean existsByInstrumentIdAndTradeDate(Long instrumentId, LocalDate tradeDate);

    /**
     * Find the latest candle for an instrument.
     *
     * @param instrumentId the instrument ID
     * @return the most recent candle or null
     */
    @Query("SELECT c FROM Candle c WHERE c.instrument.id = :instrumentId ORDER BY c.tradeDate DESC LIMIT 1")
    Candle findLatestCandle(@Param("instrumentId") Long instrumentId);

    /**
     * Delete candles for an instrument within a date range.
     *
     * @param instrumentId the instrument ID
     * @param startDate    the start date (inclusive)
     * @param endDate      the end date (inclusive)
     */
    void deleteByInstrumentIdAndTradeDateBetween(Long instrumentId, LocalDate startDate, LocalDate endDate);
}
