package com.payshield.client;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class LoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        System.out.println("URI: " + request.getURI());
        System.out.println("Headers: " + request.getHeaders());
        System.out.println("Body bytes: " + body.length);
        System.out.println("Body: " + new String(body, StandardCharsets.UTF_8));
        return execution.execute(request, body);
    }
}