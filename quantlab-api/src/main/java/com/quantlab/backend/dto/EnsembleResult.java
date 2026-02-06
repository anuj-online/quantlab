package com.quantlab.backend.dto;

import java.util.List;

public class EnsembleResult {
    private List<EnsembleSignal> signals;

    public EnsembleResult() {}

    public EnsembleResult(List<EnsembleSignal> signals) {
        this.signals = signals;
    }

    // Getter and Setter
    public List<EnsembleSignal> getSignals() { return signals; }
    public void setSignals(List<EnsembleSignal> signals) { this.signals = signals; }
}