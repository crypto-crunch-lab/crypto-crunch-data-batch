package com.crypto.crunch.batch.common.feign.client;

import com.crypto.crunch.batch.defi.model.ImageUploadRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "coreApiClient")
@RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public interface CoreApiClient {
    @PostMapping("/api/v1/image/upload")
    ResponseEntity<String> uploadImage(@RequestBody ImageUploadRequest request);
}
