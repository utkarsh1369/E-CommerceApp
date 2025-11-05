package com.microservices.order_service.feign;

import com.microservices.order_service.exception.ProductServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign error: methodKey={}, status={}", methodKey, response.status());

        if (methodKey.contains("ProductClient")) {
            if (response.status() == 404) {
                return new ProductServiceException("Product not found in Product Service");
            } else if (response.status() >= 500) {
                return new ProductServiceException("Product Service is unavailable");
            }
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
