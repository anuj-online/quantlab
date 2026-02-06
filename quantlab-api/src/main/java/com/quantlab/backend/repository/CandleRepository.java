package com.quantlab.backend.repository;

import com.quantlab.backend.entity.Candle;
import org.springframework.data.domain.Pageable;
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

    /**
     * Find the last N candles for an instrument, ordered by trade date ascending.
     * <p>
     * This method is optimized for screening mode where only recent candles are needed
     * for indicator calculation. Returns candles in chronological order (oldest first).
     *
     * @param instrumentId the instrument ID
     * @param limit        maximum number of candles to return
     * @return list of the most recent candles ordered by trade date ascending
     */
    @Query("SELECT c FROM Candle c WHERE c.instrument.id = :instrumentId ORDER BY c.tradeDate DESC")
    List<Candle> findLatestCandles(@Param("instrumentId") Long instrumentId, Pageable pageable);

    /**
     * Find candles for a specific instrument up to a given date, limited to N most recent.
     * <p>
     * This is useful for screening mode where we want candles up to a specific date
     * (run date) but only need the last N candles for indicator calculation.
     *
     * @param instrumentId the instrument ID
     * @param maxDate      the maximum trade date (inclusive)
     * @param limit        maximum number of candles to return
     * @return list of candles up to maxDate, ordered by trade date ascending
     */
    @Query("SELECT c FROM Candle c WHERE c.instrument.id = :instrumentId AND c.tradeDate <= :maxDate ORDER BY c.tradeDate DESC")
    List<Candle> findCandlesUpToDate(@Param("instrumentId") Long instrumentId, @Param("maxDate") LocalDate maxDate, Pageable pageable);

    /**
     * Check existence of candles for multiple instrument-date pairs.
     * Used for batch idempotent checks during parallel Bhavcopy loading.
     *
     * @param instrumentIds list of instrument IDs
     * @param tradeDates    corresponding list of trade dates
     * @return list of instrument IDs that already have candles for the given dates
     */
    @Query("SELECT DISTINCT c.instrument.id FROM Candle c WHERE c.instrument.id IN :instrumentIds AND c.tradeDate IN :tradeDates")
    List<Long> findExistingInstrumentIds(@Param("instrumentIds") List<Long> instrumentIds, @Param("tradeDates") List<LocalDate> tradeDates);
}
