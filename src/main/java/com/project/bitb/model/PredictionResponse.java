package com.project.bitb.model;

public class PredictionResponse {
    private int prediction;
    private String label;

    public PredictionResponse(int prediction, String label) {
        this.prediction = prediction;
        this.label = label;
    }

    public int getPrediction() {
        return prediction;
    }

    public String getLabel() {
        return label;
    }
}
