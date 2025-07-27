package com.project.bitb.service;

import ai.onnxruntime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

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
                float[] logits;

                Object raw = result.get(0).getValue();
                if (raw instanceof float[][]) {
                    logits = ((float[][]) raw)[0];
                } else {
                    throw new RuntimeException("Unsupported output type: " + raw.getClass());
                }

                logger.info("📤 ONNX logits: {}", Arrays.toString(logits));

                float[] probs = softmax(logits);
                int predictedClass = maxIndex(probs);
                float score = probs[predictedClass];

                logger.info("📊 Softmax probs: {}", Arrays.toString(probs));
                logger.info("✅ Predicted class: {}, Score: {}", predictedClass, score);

                String message = switch (predictedClass) {
                    case 0 -> "✅ Page looks clean";
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
            probs[i] = (float)(exps[i] / sum);
        }
        return probs;
    }

    private int maxIndex(float[] arr) {
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[maxIdx]) maxIdx = i;
        }
        return maxIdx;
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
