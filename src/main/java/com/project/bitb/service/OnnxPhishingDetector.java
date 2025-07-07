package com.project.bitb.service;

import ai.onnxruntime.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OnnxPhishingDetector {

    private final OrtEnvironment env;
    private final OrtSession session;

    public OnnxPhishingDetector() {
        try {
            this.env = OrtEnvironment.getEnvironment();
            // Load model from resources folder
            File modelFile = new ClassPathResource("models/bitb_phishing_detector.onnx").getFile();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            this.session = env.createSession(modelFile.getAbsolutePath(), opts);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ONNX model: " + e.getMessage(), e);
        }
    }

    public int predict(List<Double> features) {
        try {
            float[] inputArray = new float[features.size()];
            for (int i = 0; i < features.size(); i++) {
                inputArray[i] = features.get(i).floatValue();
            }

            float[][] inputTensor = new float[1][];
            inputTensor[0] = inputArray;

            OnnxTensor tensor = OnnxTensor.createTensor(env, inputTensor);
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("float_input", tensor); // Match with your ONNX model input name

            try (OrtSession.Result result = session.run(inputs)) {
                Object outputObj = result.get(0).getValue();
                System.out.println("Model output type: " + outputObj.getClass());

                if (outputObj instanceof long[]) {
                    return (int) ((long[]) outputObj)[0];
                } else if (outputObj instanceof long[][]) {
                    return (int) ((long[][]) outputObj)[0][0];
                } else if (outputObj instanceof float[]) {
                    return Math.round(((float[]) outputObj)[0]);
                } else if (outputObj instanceof float[][]) {
                    return Math.round(((float[][]) outputObj)[0][0]);
                } else {
                    throw new RuntimeException("Unexpected model output type: " + outputObj.getClass());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }
}
