package com.crypto.crunch.batch.common.feign;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class FeignErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultErrorDecoder = new Default();
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() >= 400 && response.status() <= 499) {
            log.error("client error => status:{}, reason:{}", response.status(), response.reason());
            return new ResponseStatusException(HttpStatus.valueOf(response.status()), response.reason());
        }
        if (response.status() >= 500 && response.status() <= 599) {
            log.error("server error => status:{}, reason:{}", response.status(), response.reason());
            return new ResponseStatusException(HttpStatus.valueOf(response.status()), response.reason());
        }
        return defaultErrorDecoder.decode(methodKey, response);
    }
}
