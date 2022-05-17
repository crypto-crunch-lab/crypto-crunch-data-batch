package com.crypto.crunch.batch.defi;

import com.crypto.crunch.batch.defi.model.Defi;
import com.crypto.crunch.batch.defi.model.DefiPlatform;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DefiPlatformUpdateServiceImpl implements DefiPlatformUpdateService {
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;

    private static final String DEFI_INDEX = "defi";
    private static final String DEFI_PLATFORM_INDEX = "defi_platform";

    public DefiPlatformUpdateServiceImpl(RestHighLevelClient restHighLevelClient, ObjectMapper objectMapper) {
        this.restHighLevelClient = restHighLevelClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void updatePlatform() throws IOException {
        SearchRequest searchRequest = new SearchRequest(DEFI_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(5000);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        List<String> emptyList = new ArrayList<>();

        List<Defi> defiList = Stream.of(searchResponse.getHits().getHits())
                .map(SearchHit::getSourceAsString)
                .map(str -> {
                    try {
                        Defi defi = objectMapper.readValue(str, Defi.class);
                        String platformName = defi.getPlatform().getName();

                        DefiPlatform platform = searchPlatform(platformName, emptyList);
                        if (StringUtils.isNotEmpty(platform.getId())) {
                            defi.setPlatform(platform);
                        }
                        return defi;
                    } catch (IOException e) {
                        log.error("error: {}", e.getMessage());
                        return null;
                    }
                })
                .collect(Collectors.toList());

        for (String platformName : emptyList) {
            DefiPlatform platform = DefiPlatform.builder().id(UUID.randomUUID().toString()).name(platformName).build();
            IndexRequest indexRequest = new IndexRequest(DEFI_PLATFORM_INDEX).id(platform.getId());
            indexRequest.source(objectMapper.writeValueAsString(platform), XContentType.JSON);
            IndexResponse indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            log.info(indexResponse.toString());
        }

        for (Defi defi : defiList) {
            UpdateRequest updateRequest = new UpdateRequest(DEFI_INDEX, defi.getId());
            updateRequest.doc(objectMapper.writeValueAsString(defi), XContentType.JSON);
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        }
    }

    private DefiPlatform searchPlatform(String platformName, List<String> emptyList) throws IOException {
        SearchRequest request = new SearchRequest(DEFI_PLATFORM_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("name.keyword", platformName));
        searchSourceBuilder.size(5000);
        request.source(searchSourceBuilder);

        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        if (response.getHits().getHits().length > 0) {
            return objectMapper.readValue(response.getHits().getHits()[0].getSourceAsString(), DefiPlatform.class);
        } else {
            if (!emptyList.contains(platformName)) {
                emptyList.add(platformName);
            }
            return new DefiPlatform();
        }
    }
}
