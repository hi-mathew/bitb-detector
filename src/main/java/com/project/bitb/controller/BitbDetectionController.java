package com.project.bitb.controller;

import com.project.bitb.model.PredictionResponse;
import com.project.bitb.service.OnnxPhishingDetector;
import com.project.bitb.service.OnnxPhishingDetector.PhishingResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bitb")
@CrossOrigin(origins = "*")
public class BitbDetectionController {

    private final OnnxPhishingDetector detector;

    public BitbDetectionController(OnnxPhishingDetector detector) {
        this.detector = detector;
    }

    @PostMapping("/detect")
    public ResponseEntity<PredictionResponse> detectBitbAttack(@RequestBody Map<String, List<Double>> requestBody) {
        List<Double> features = requestBody.get("features");

        if (features == null || features.size() != 3) {
            return ResponseEntity
                    .badRequest()
                    .body(new PredictionResponse(-1, 0.0f, "Invalid input. Expected 3 features."));
        }

        PhishingResult result = detector.predict(features);
        String label = result.prediction == 1 ? "BitB Attack Detected" : "No BitB Attack";

        return ResponseEntity.ok(new PredictionResponse(result.prediction, result.score, label));
    }
}
