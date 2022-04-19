package com.crypto.crunch.batch.defi;

import com.crypto.crunch.batch.defi.model.CoinDixApiResponse;
import com.crypto.crunch.batch.defi.model.CoinDixDefi;
import com.crypto.crunch.batch.defi.model.Defi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DefiSyncServiceImpl implements DefiSyncService {

    private final CoinDixApiClient coinDixApiClient;
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;

    public DefiSyncServiceImpl(CoinDixApiClient coinDixApiClient, RestHighLevelClient restHighLevelClient, ObjectMapper objectMapper) {
        this.coinDixApiClient = coinDixApiClient;
        this.restHighLevelClient = restHighLevelClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sync() throws IOException {
        int pageNo = 0;
        List<Defi> defiList = new ArrayList<>();
        while (true) {
            CoinDixApiResponse response = coinDixApiClient.getList(pageNo);
            List<Defi> currList = response.getData().stream().map(this::mapToDefi).collect(Collectors.toList());
            defiList.addAll(currList);
            pageNo++;

            if (!response.getHasNextPage()) {
                break;
            }
        }
        log.info("total count: {}", defiList.size());


        BulkRequest bulkRequest = new BulkRequest();

        for (Defi defi : defiList) {
            IndexRequest indexRequest = new IndexRequest("defi").id(defi.getId());
            try {
                indexRequest.source(objectMapper.writeValueAsString(defi), XContentType.JSON);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            bulkRequest.add(indexRequest);
        }

        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        log.info(bulkResponse.toString());
    }

    private Defi mapToDefi(CoinDixDefi coinDixDefi) {
        return Defi.builder()
                .id(coinDixDefi.getId())
                .name(coinDixDefi.getName())
                .platform(coinDixDefi.getProtocol())
                .network(coinDixDefi.getChain())
                .base(coinDixDefi.getBase())
                .reward(coinDixDefi.getReward())
                .apy(coinDixDefi.getApy())
                .tvl(coinDixDefi.getTvl())
                .risk(coinDixDefi.getRisk())
                .icon(coinDixDefi.getIcon())
                .link(coinDixDefi.getLink())
                .build();
    }
}
