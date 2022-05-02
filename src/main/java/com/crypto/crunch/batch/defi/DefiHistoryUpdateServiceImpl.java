package com.crypto.crunch.batch.defi;

import com.crypto.crunch.batch.common.feign.client.CoinDixApiClient;
import com.crypto.crunch.batch.defi.model.Defi;
import com.crypto.crunch.batch.defi.model.DefiHistory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DefiHistoryUpdateServiceImpl implements DefiHistoryUpdateService {

    private final CoinDixApiClient coinDixApiClient;
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;

    private final String API_AUTH_KEY = "Bearer 53ecc5e2d1a4c66fda5f6f90d7c55e4e93be5bd7";

    public DefiHistoryUpdateServiceImpl(CoinDixApiClient coinDixApiClient, RestHighLevelClient restHighLevelClient, ObjectMapper objectMapper) {
        this.coinDixApiClient = coinDixApiClient;
        this.restHighLevelClient = restHighLevelClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void updateSeries() throws IOException {
        SearchRequest searchRequest = new SearchRequest("defi");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(5000);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<Defi> defiList = Stream.of(searchResponse.getHits().getHits())
                .map(SearchHit::getSourceAsString)
                .map(str -> {
                    try {
                        Defi defi = objectMapper.readValue(str, Defi.class);
                        String id = defi.getId();
                        try {
                            List<DefiHistory> histories = objectMapper.readValue(objectMapper.writeValueAsString(coinDixApiClient.getSeries(API_AUTH_KEY, id, 365).get("series")), new TypeReference<List<DefiHistory>>() {});
                            defi.setHistories(histories);
                            return defi;
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        return defi;
                    } catch (IOException e) {
                        log.error("error: {}", e.getMessage());
                        return null;
                    }
                })
                .collect(Collectors.toList());

        for (Defi defi : defiList) {
            UpdateRequest updateRequest = new UpdateRequest("defi", defi.getId());
            updateRequest.doc(objectMapper.writeValueAsString(defi), XContentType.JSON);
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        }
    }
}
