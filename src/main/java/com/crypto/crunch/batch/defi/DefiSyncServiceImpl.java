package com.crypto.crunch.batch.defi;

import com.crypto.crunch.batch.common.feign.client.CoinDixApiClient;
import com.crypto.crunch.batch.common.feign.client.CoreApiClient;
import com.crypto.crunch.batch.defi.model.*;
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
import org.springframework.util.ObjectUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DefiSyncServiceImpl implements DefiSyncService {

    private final CoinDixApiClient coinDixApiClient;
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;
    private final CoreApiClient coreApiClient;

    public DefiSyncServiceImpl(CoinDixApiClient coinDixApiClient, RestHighLevelClient restHighLevelClient, ObjectMapper objectMapper, CoreApiClient coreApiClient) {
        this.coinDixApiClient = coinDixApiClient;
        this.restHighLevelClient = restHighLevelClient;
        this.objectMapper = objectMapper;
        this.coreApiClient = coreApiClient;
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

        BulkResponse response = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        log.info(response.toString());
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
                .defiIconUrl(coinDixDefi.getIcon())
                .detailUrl(coinDixDefi.getLink())
                .build();
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
