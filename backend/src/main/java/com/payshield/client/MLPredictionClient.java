package com.payshield.client;

import com.payshield.dto.ml.FraudPredictionRequest;
import com.payshield.dto.ml.FraudPredictionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Component
public class MLPredictionClient {

    private final RestClient restClient;

    public MLPredictionClient(@Value("http://localhost:8000") String mlServiceUrl) {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        this.restClient = RestClient.builder()
                .baseUrl(mlServiceUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .requestInterceptor(new LoggingInterceptor())
                .build();
    }

    public FraudPredictionResponse predict(FraudPredictionRequest request) {

        System.out.println("=================================");
        System.out.println("Sending ML request:");
        System.out.println(request);
        System.out.println("=================================");

        return restClient
                .post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(FraudPredictionResponse.class);
    }
}