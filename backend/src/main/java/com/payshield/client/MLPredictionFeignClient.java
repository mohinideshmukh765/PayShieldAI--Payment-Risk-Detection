package com.payshield.client;

import com.payshield.dto.ml.FraudPredictionRequest;
import com.payshield.dto.ml.FraudPredictionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ml-service", url = "${ml.service.url}")
public interface MLPredictionFeignClient {

    @PostMapping("/predict")
    FraudPredictionResponse predict(@RequestBody FraudPredictionRequest request);
}