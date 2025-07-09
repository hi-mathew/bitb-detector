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
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            this.session = env.createSession(modelFile.getAbsolutePath(), opts);

            // Optional: Log model input/output details
            logger.info("ONNX Model loaded: {}", modelFile.getName());
            logger.info("Model Input Names: {}", session.getInputNames());
            logger.info("Model Output Names: {}", session.getOutputNames());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ONNX model: " + e.getMessage(), e);
        }
    }

    public PhishingResult predict(List<Double> features) {
        try {
            // Prepare input array
            float[] inputArray = new float[features.size()];
            for (int i = 0; i < features.size(); i++) {
                inputArray[i] = features.get(i).floatValue();
            }

            float[][] inputTensor = new float[1][];
            inputTensor[0] = inputArray;

            OnnxTensor tensor = OnnxTensor.createTensor(env, inputTensor);

            Map<String, OnnxTensor> inputs = new HashMap<>();
            String inputName = session.getInputNames().iterator().next();
            inputs.put(inputName, tensor);

            try (OrtSession.Result result = session.run(inputs)) {
                Object output = result.get(0).getValue();

                float score = -1f;
                int prediction;

                if (output instanceof float[][] out2D) {
                    score = out2D[0][0];
                    prediction = score >= 0.5 ? 1 : 0;
                } else if (output instanceof float[] out1D) {
                    score = out1D[0];
                    prediction = score >= 0.5 ? 1 : 0;
                } else if (output instanceof long[][] out2DLong) {
                    prediction = (int) out2DLong[0][0];
                    score = prediction;
                } else if (output instanceof long[] out1DLong) {
                    prediction = (int) out1DLong[0];
                    score = prediction;
                } else {
                    throw new RuntimeException("Unsupported ONNX output type: " + output.getClass());
                }

                logger.info("ONNX input: {}", features);
                logger.info("ONNX raw output: {}", output);
                logger.info("Predicted: {}, Score: {}", prediction, score);

                return new PhishingResult(prediction, score);
            }

        } catch (Exception e) {
            logger.error("ONNX prediction failed", e);
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }


    public static class PhishingResult {
        public final int prediction; // 0 = safe, 1 = phishing
        public final float score;    // raw score or probability

        public PhishingResult(int prediction, float score) {
            this.prediction = prediction;
            this.score = score;
        }

        @Override
        public String toString() {
            return "PhishingResult{" +
                    "prediction=" + (prediction == 1 ? "Phishing" : "Safe") +
                    ", score=" + score +
                    '}';
        }
    }
}
