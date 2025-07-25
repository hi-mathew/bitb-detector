package com.project.bitb.service;

import ai.onnxruntime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put(inputName, OnnxTensor.createTensor(env, inputTensor));

            try (OrtSession.Result result = session.run(inputs)) {
                Object output = result.get(0).getValue();

                float score = 0.0f;
                int prediction = 0;

                if (output instanceof float[][]) {
                    score = ((float[][]) output)[0][0];
                    prediction = score > 0.80f ? 1 : 0;
                } else if (output instanceof float[]) {
                    score = ((float[]) output)[0];
                    prediction = score > 0.80f ? 1 : 0;
                } else if (output instanceof long[][]) {
                    long val = ((long[][]) output)[0][0];
                    prediction = (int) val;
                    score = (float) val;
                } else if (output instanceof long[]) {
                    long val = ((long[]) output)[0];
                    prediction = (int) val;
                    score = (float) val;
                } else {
                    throw new RuntimeException("Unsupported output type: " + output.getClass());
                }

                logger.info("📤 ONNX raw output: {}", output);
                logger.info("✅ Predicted: {}, Score: {}", prediction, score);

                return new PhishingResult(prediction, score);
            }

        } catch (Exception e) {
            logger.error("❌ ONNX prediction failed", e);
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }

    public static class PhishingResult {
        public final int prediction;
        public final float score;

        public PhishingResult(int prediction, float score) {
            this.prediction = prediction;
            this.score = score;
        }
    }
}
