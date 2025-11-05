package com.microservices.delivery_service.feign;

import com.microservices.delivery_service.exception.OrderServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign error: methodKey={}, status={}", methodKey, response.status());

        if (methodKey.contains("OrderClient")) {
            if (response.status() == 404) {
                return new OrderServiceException("Order not found in Order Service");
            } else if (response.status() >= 500) {
                return new OrderServiceException("Order Service is unavailable");
            }
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
