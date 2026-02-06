package com.quantlab.backend.entity;

/**
 * Status enum for trade signals
 * Signals are generated trades that haven't been executed yet
 */
public enum TradeSignalStatus {
    PENDING,    // Signal generated, not entered yet
    IGNORED,    // Screening signal not taken
    EXECUTED    // Signal converted to actual trade
}