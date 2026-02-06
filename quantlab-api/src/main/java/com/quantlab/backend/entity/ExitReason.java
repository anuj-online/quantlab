package com.quantlab.backend.entity;

/**
 * Exit reason enum for closed trades
 * Tracks why a trade was closed
 */
public enum ExitReason {
    STOP_LOSS,  // Hit stop loss level
    TARGET,     // Hit target price
    TIME,       // Time-based exit (end of period, etc.)
    MANUAL      // Manual exit
}