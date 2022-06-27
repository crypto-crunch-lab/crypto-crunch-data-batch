package com.crypto.crunch.batch.defi;

import com.crypto.crunch.batch.common.feign.client.CoinDixApiClient;
import com.crypto.crunch.batch.common.feign.client.CoreApiClient;
import com.crypto.crunch.batch.defi.conf.DefiConf;
import com.crypto.crunch.batch.defi.conf.ImageConf;
import com.crypto.crunch.batch.defi.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DefiSyncServiceImpl implements DefiSyncService {

    private final CoinDixApiClient coinDixApiClient;
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;
    private final CoreApiClient coreApiClient;

    private static final String YMDT_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String DEFI_INDEX = "defi";
    private static final String DEFI_PLATFORM_INDEX = "defi_platform";

    private static final Map<DefiConf.DefiCoinType, List<Defi>> listMap;

    static {
        listMap = new HashMap<>();
    }

    public DefiSyncServiceImpl(CoinDixApiClient coinDixApiClient, RestHighLevelClient restHighLevelClient, ObjectMapper objectMapper, CoreApiClient coreApiClient) {
        this.coinDixApiClient = coinDixApiClient;
        this.restHighLevelClient = restHighLevelClient;
        this.objectMapper = objectMapper;
        this.coreApiClient = coreApiClient;
    }

    @Override
    public void sync() throws IOException {
        String now = DateTime.now().toString(YMDT_TIME_FORMAT);
        this.initializeListMap();

        List<Defi> defiList = this.getList();

        int createCount = 0;
        int updateCount = 0;
        BulkRequest bulkRequest = new BulkRequest();
        for (Defi defi : defiList) {
            GetRequest getRequest = new GetRequest(DEFI_INDEX, defi.getId());
            GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
            if (getResponse.isExists()) {
                defi.setUpdateYmdt(now);

                UpdateRequest updateRequest = new UpdateRequest(DEFI_INDEX, defi.getId());
                updateRequest.doc(objectMapper.writeValueAsString(defi), XContentType.JSON);
                bulkRequest.add(updateRequest);
                updateCount++;
            } else {
                defi.setSyncYmdt(now);
                defi.setUpdateYmdt(now);
                defi.setIsService(true);
                defi.setIsRecommend(false);

                IndexRequest indexRequest = new IndexRequest(DEFI_INDEX).id(defi.getId());
                indexRequest.source(objectMapper.writeValueAsString(defi), XContentType.JSON);
                bulkRequest.add(indexRequest);
                createCount++;
            }
        }
        log.info("update count: {}", updateCount);
        log.info("create count: {}", createCount);
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            log.debug("bulk fail, bulkResponse: {}", bulkResponse.toString());
        }
    }

    private void initializeListMap() {
        listMap.put(DefiConf.DefiCoinType.LP_TOKEN, this.getListByCoinType(DefiConf.DefiCoinType.LP_TOKEN));
        listMap.put(DefiConf.DefiCoinType.SINGLE_COIN, this.getListByCoinType(DefiConf.DefiCoinType.SINGLE_COIN));
        listMap.put(DefiConf.DefiCoinType.STABLE_COIN, this.getListByCoinType(DefiConf.DefiCoinType.STABLE_COIN));
        listMap.put(DefiConf.DefiCoinType.NO_IMPERMANENT_LOSS, this.getListByCoinType(DefiConf.DefiCoinType.NO_IMPERMANENT_LOSS));
    }

    private Defi mapToDefi(CoinDixDefi coinDixDefi) {
        try {
            return Defi.builder()
                    .id(coinDixDefi.getId())
                    .name(coinDixDefi.getName())
                    .platform(this.getPlatform(coinDixDefi.getProtocol()))
                    .network(DefiNetwork.builder().name(coinDixDefi.getChain()).networkIconUrl(String.format("https://coindix.com/img/chains/%s.svg", coinDixDefi.getChain().replaceFirst(" ", "").toLowerCase())).build())
                    .base(coinDixDefi.getBase())
                    .reward(coinDixDefi.getReward())
                    .apy(coinDixDefi.getApy())
                    .tvl(coinDixDefi.getTvl())
                    .risk(coinDixDefi.getRisk())
                    .defiIconUrl(coinDixDefi.getIcon())
                    .detailUrl(coinDixDefi.getLink())
                    .coinTypes(this.getCoinTypes(coinDixDefi.getId()))
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private DefiPlatform getPlatform(String platformName) throws IOException {
        SearchRequest searchRequest = new SearchRequest(DEFI_PLATFORM_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("name.keyword", platformName));
        searchRequest.source(searchSourceBuilder);

        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        if (response.getHits().getTotalHits().value == 0) {
            return DefiPlatform.builder().name(platformName).build();
        }

        return Stream.of(response.getHits().getHits())
                .map(SearchHit::getSourceAsString)
                .map(str -> {
                    try {
                        return objectMapper.readValue(str, DefiPlatform.class);
                    } catch (IOException e) {
                        log.error("error: {}", e.getMessage());
                        return null;
                    }
                })
                .collect(Collectors.toList()).get(0);
    }

    private List<DefiConf.DefiCoinType> getCoinTypes(String defiId) {
        List<DefiConf.DefiCoinType> coinTypeList = new ArrayList<>();

        if (listMap.get(DefiConf.DefiCoinType.LP_TOKEN) != null) {
            listMap.get(DefiConf.DefiCoinType.LP_TOKEN)
                    .stream()
                    .filter(d -> StringUtils.equals(d.getId(), defiId))
                    .findAny()
                    .ifPresent(b -> coinTypeList.add(DefiConf.DefiCoinType.LP_TOKEN));
        }
        if (listMap.get(DefiConf.DefiCoinType.SINGLE_COIN) != null) {
            listMap.get(DefiConf.DefiCoinType.SINGLE_COIN)
                    .stream()
                    .filter(d -> StringUtils.equals(d.getId(), defiId))
                    .findAny()
                    .ifPresent(b -> coinTypeList.add(DefiConf.DefiCoinType.SINGLE_COIN));
        }
        if (listMap.get(DefiConf.DefiCoinType.STABLE_COIN) != null) {
            listMap.get(DefiConf.DefiCoinType.STABLE_COIN)
                    .stream()
                    .filter(d -> StringUtils.equals(d.getId(), defiId))
                    .findAny()
                    .ifPresent(b -> coinTypeList.add(DefiConf.DefiCoinType.STABLE_COIN));
        }
        if (listMap.get(DefiConf.DefiCoinType.NO_IMPERMANENT_LOSS) != null) {
            listMap.get(DefiConf.DefiCoinType.NO_IMPERMANENT_LOSS)
                    .stream()
                    .filter(d -> StringUtils.equals(d.getId(), defiId))
                    .findAny()
                    .ifPresent(b -> coinTypeList.add(DefiConf.DefiCoinType.NO_IMPERMANENT_LOSS));
        }

        return coinTypeList;
    }

    private List<Defi> getList() {
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
        log.info("defiList count: {}", defiList.size());
        return defiList;
    }

    private List<Defi> getListByCoinType(DefiConf.DefiCoinType coinType) {
        int pageNo = 0;
        List<Defi> defiList = new ArrayList<>();
        while (true) {
            CoinDixApiResponse response = coinDixApiClient.getListByCoinType(
                    coinType == DefiConf.DefiCoinType.LP_TOKEN ? "lp" :
                            coinType == DefiConf.DefiCoinType.SINGLE_COIN ? "single" :
                                    coinType == DefiConf.DefiCoinType.STABLE_COIN ? "stable" :
                                            "noimploss", pageNo);
            List<Defi> currList = response.getData().stream().map(this::mapToDefi).collect(Collectors.toList());
            defiList.addAll(currList);
            pageNo++;

            if (!response.getHasNextPage()) {
                break;
            }
        }
        return defiList;
    }

    private String getDefiIconUrl(String id, String url) {
        try {
            BufferedImage image = ImageIO.read(new URL("https://crypto-crunch-static.s3.ap-northeast-2.amazonaws.com/image/icon/defi/" + id + ".png"));
            if (!ObjectUtils.isEmpty(image)) {
                return "https://crypto-crunch-static.s3.ap-northeast-2.amazonaws.com/image/icon/defi/" + id + ".png";
            }
        } catch (IOException e) {
            log.info("image file not exist");
        }

        ImageUploadRequest request = new ImageUploadRequest();
        request.setUrl(url);
        request.setUploadType(ImageConf.ImageUploadType.URL);
        request.setDirName("image/icon/defi");
        request.setFileName(id + ".png");

        return coreApiClient.uploadImage(request).getBody();
    }
}
