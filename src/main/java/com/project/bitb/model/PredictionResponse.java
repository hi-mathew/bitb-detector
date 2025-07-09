package com.project.bitb.model;

public class PredictionResponse {
    private int prediction;
    private float score;
    private String message;

    public PredictionResponse(int prediction, float score, String message) {
        this.prediction = prediction;
        this.score = score;
        this.message = message;
    }

    // Getters and Setters
    public int getPrediction() {
        return prediction;
    }

    public void setPrediction(int prediction) {
        this.prediction = prediction;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
