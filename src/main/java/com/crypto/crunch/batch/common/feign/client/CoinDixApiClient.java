package com.crypto.crunch.batch.common.feign.client;

import com.crypto.crunch.batch.defi.model.CoinDixApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "coinDixApiClient")
@RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public interface CoinDixApiClient {
    @GetMapping("/search?sort=-tvl")
    CoinDixApiResponse getList(@RequestParam("page") int page);
}