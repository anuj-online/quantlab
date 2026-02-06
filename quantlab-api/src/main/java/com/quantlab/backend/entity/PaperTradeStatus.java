package com.quantlab.backend.entity;

/**
 * Status enum for paper trades
 * Represents the execution status of trades
 */
public enum PaperTradeStatus {
    OPEN,       // Position is active
    CLOSED      // Position has been exited
}