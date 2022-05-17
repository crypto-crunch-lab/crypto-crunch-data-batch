package com.crypto.crunch.batch.defi;

import com.crypto.crunch.batch.common.feign.client.CoinDixApiClient;
import com.crypto.crunch.batch.defi.conf.DefiConf;
import com.crypto.crunch.batch.defi.model.CoinDixDefiHistory;
import com.crypto.crunch.batch.defi.model.Defi;
import com.crypto.crunch.batch.defi.model.DefiHistory;
import com.crypto.crunch.batch.defi.model.DefiSeries;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DefiHistoryUpdateServiceImpl implements DefiHistoryUpdateService {

    private final CoinDixApiClient coinDixApiClient;
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;

    private final String API_AUTH_KEY = "Bearer 53ecc5e2d1a4c66fda5f6f90d7c55e4e93be5bd7";
    private static final String YMDT_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String DEFI_INDEX = "defi";

    public DefiHistoryUpdateServiceImpl(CoinDixApiClient coinDixApiClient, RestHighLevelClient restHighLevelClient, ObjectMapper objectMapper) {
        this.coinDixApiClient = coinDixApiClient;
        this.restHighLevelClient = restHighLevelClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void updateSeries() throws IOException {
        String now = DateTime.now().toString(YMDT_TIME_FORMAT);

        SearchRequest searchRequest = new SearchRequest(DEFI_INDEX);
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
                            List<CoinDixDefiHistory> coinDixHistories = objectMapper.readValue(
                                    objectMapper.writeValueAsString(coinDixApiClient.getSeries(API_AUTH_KEY, id, 365).get("series")),
                                    new TypeReference<List<CoinDixDefiHistory>>() {
                                    }
                            );

                            DefiSeries<Double> apySeries = new DefiSeries<>();
                            List<DefiHistory<Double>> apyHistories = coinDixHistories
                                    .stream()
                                    .map(h -> DefiHistory.<Double>builder()
                                            .historyType(DefiConf.DefiHistoryType.APY)
                                            .value(h.getApy())
                                            .syncYmd(h.getDate().substring(0, 10))
                                            .build()
                                    )
                                    .collect(Collectors.toList());

                            apySeries.setHistories(apyHistories);
                            apySeries.setMaxValue(apyHistories.stream().mapToDouble(DefiHistory::getValue).max().orElseThrow(NoSuchElementException::new));
                            apySeries.setMinValue(apyHistories.stream().mapToDouble(DefiHistory::getValue).min().orElseThrow(NoSuchElementException::new));
                            apySeries.setStartYmd(apyHistories.get(0).getSyncYmd());
                            apySeries.setEndYmd(apyHistories.get(apyHistories.size() - 1).getSyncYmd());
                            apySeries.setCount(apyHistories.size());
                            defi.setApySeries(apySeries);

                            DefiSeries<Long> tvlSeries = new DefiSeries<>();
                            List<DefiHistory<Long>> tvlHistories = coinDixHistories
                                    .stream()
                                    .map(h -> DefiHistory.<Long>builder()
                                            .historyType(DefiConf.DefiHistoryType.TVL)
                                            .value(h.getTvl())
                                            .syncYmd(h.getDate().substring(0, 10))
                                            .build()
                                    )
                                    .collect(Collectors.toList());

                            tvlSeries.setHistories(tvlHistories);
                            tvlSeries.setMaxValue(tvlHistories.stream().mapToLong(DefiHistory::getValue).max().orElseThrow(NoSuchElementException::new));
                            tvlSeries.setMinValue(tvlHistories.stream().mapToLong(DefiHistory::getValue).min().orElseThrow(NoSuchElementException::new));
                            tvlSeries.setStartYmd(tvlHistories.get(0).getSyncYmd());
                            tvlSeries.setEndYmd(tvlHistories.get(tvlHistories.size() - 1).getSyncYmd());
                            tvlSeries.setCount(tvlHistories.size());
                            defi.setTvlSeries(tvlSeries);

                            defi.setHistoryUpdateYmdt(now);
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

        log.info("update count: {}", defiList.size());
        for (Defi defi : defiList) {
            UpdateRequest updateRequest = new UpdateRequest(DEFI_INDEX, defi.getId());
            updateRequest.doc(objectMapper.writeValueAsString(defi), XContentType.JSON);
            UpdateResponse updateResponse = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        }
    }
}
