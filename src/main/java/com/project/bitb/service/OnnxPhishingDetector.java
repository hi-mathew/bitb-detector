package com.project.bitb.service;

import ai.onnxruntime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class OnnxPhishingDetector {

    private static final Logger logger = LoggerFactory.getLogger(OnnxPhishingDetector.class);
    private final OrtEnvironment env;
    private final OrtSession session;

    public OnnxPhishingDetector() {
        try {
            this.env = OrtEnvironment.getEnvironment();
            File modelFile = new ClassPathResource("models/bitb_phishing_detector.onnx").getFile();
            this.session = env.createSession(modelFile.getAbsolutePath(), new OrtSession.SessionOptions());
            logger.info("✅ ONNX model loaded: {}", modelFile.getName());
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to load ONNX model: " + e.getMessage(), e);
        }
    }

    public PhishingResult predict(List<Double> features) {
        try {
            logger.info("📥 Input features: {}", features);

            float[][] inputTensor = new float[1][features.size()];
            for (int i = 0; i < features.size(); i++) {
                inputTensor[0][i] = features.get(i).floatValue();
            }

            String inputName = session.getInputNames().iterator().next();
            Map<String, OnnxTensor> inputs = Map.of(inputName, OnnxTensor.createTensor(env, inputTensor));

            try (OrtSession.Result result = session.run(inputs)) {
                Object raw = result.get(0).getValue();

                int predictedClass;
                float score;

                if (raw instanceof float[][] logitsArray) {
                    float[] logits = logitsArray[0];
                    logger.info("📤 ONNX logits: {}", Arrays.toString(logits));

                    float[] probs = softmax(logits);
                    logger.info("📊 Softmax probs: {}", Arrays.toString(probs));

                    float bitbScore = probs[2];
                    float suspiciousScore = probs[1];
                    float safeScore = probs[0];

                    if (bitbScore >= 0.38f && bitbScore == max(probs)) {
                        predictedClass = 2;
                    } else if (suspiciousScore >= 0.38f && suspiciousScore == max(probs)) {
                        predictedClass = 1;
                    } else {
                        predictedClass = 0;
                    }

                    score = probs[predictedClass];

                } else if (raw instanceof long[] rawLong) {
                    logger.info("📤 ONNX output (long[]): {}", Arrays.toString(rawLong));
                    predictedClass = (int) rawLong[0]; // Cast from long to int
                    score = 1.0f; // No probability available

                } else {
                    throw new RuntimeException("❌ Unsupported output type: " + raw.getClass());
                }

                logger.info("✅ Final prediction: {}, Score: {}", predictedClass, score);

                String message = switch (predictedClass) {
                    case 0 -> "✅ Page Verified as Safe";
                    case 1 -> "⚠️ Suspicious Behavior Detected";
                    case 2 -> "🚨 BitB Attack Detected";
                    default -> "Unknown";
                };

                return new PhishingResult(predictedClass, score, message);
            }

        } catch (Exception e) {
            logger.error("❌ ONNX prediction failed", e);
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }

    private float[] softmax(float[] logits) {
        double max = Double.NEGATIVE_INFINITY;
        for (float val : logits) max = Math.max(max, val);

        double[] exps = new double[logits.length];
        double sum = 0.0;
        for (int i = 0; i < logits.length; i++) {
            exps[i] = Math.exp(logits[i] - max);
            sum += exps[i];
        }

        float[] probs = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            probs[i] = (float) (exps[i] / sum);
        }
        return probs;
    }

    private float max(float[] arr) {
        float maxVal = arr[0];
        for (float v : arr) {
            if (v > maxVal) {
                maxVal = v;
            }
        }
        return maxVal;
    }

    public static class PhishingResult {
        public final int prediction;
        public final float score;
        public final String message;

        public PhishingResult(int prediction, float score, String message) {
            this.prediction = prediction;
            this.score = score;
            this.message = message;
        }
    }
}